package com.lifeshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifeshop.entity.AccessLog;
import com.lifeshop.entity.IpBlacklist;
import com.lifeshop.entity.SecurityAlert;
import com.lifeshop.mapper.AccessLogMapper;
import com.lifeshop.mapper.IpBlacklistMapper;
import com.lifeshop.mapper.SecurityAlertMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 网络安全运维服务。
 *
 * <p>职责分四块：
 * <ol>
 *   <li>访问日志异步落库：拦截器只把日志放进无界有界队列，由独立线程批量写入，
 *       写日志不占用业务请求的处理时间；</li>
 *   <li>IP 黑名单：内存缓存 + 数据库持久化，支持自动封禁与到期失效；</li>
 *   <li>流量统计：按分钟聚合 QPS、错误率、响应时延、源 IP 分布与慢接口；</li>
 *   <li>智能异常检测：对多维指标建立 EWMA 动态基线，用标准化偏离（z-score）
 *       计算异常评分并自动生成告警，替代固定阈值告警的僵化问题。</li>
 * </ol>
 */
@Service
public class SecOpsService {

    /** 检测窗口长度（分钟） */
    private static final int DETECT_WINDOW_MINUTES = 12;
    /** EWMA 平滑系数 */
    private static final double ALPHA = 0.30;
    /** 综合异常评分告警阈值 */
    private static final double ALERT_THRESHOLD = 3.0;

    /** 各指标在综合评分中的权重 */
    private static final Map<String, Double> WEIGHTS = new LinkedHashMap<>();
    private static final Map<String, String> METRIC_NAMES = new LinkedHashMap<>();
    /**
     * 各指标的量纲尺度下限。四类指标单位不同（req/s、比例、毫秒、比例），
     * 当某指标长期稳定、方差趋近 0 时，直接用标准差做分母会放大出失真的偏离值，
     * 因此在分母上叠加一个与该指标业务量级相符的最小值。
     */
    private static final Map<String, Double> SCALE_FLOOR = new HashMap<>();

    static {
        WEIGHTS.put("qps", 0.35);
        WEIGHTS.put("errorRate", 0.30);
        WEIGHTS.put("avgCost", 0.20);
        WEIGHTS.put("ipConcentration", 0.15);

        METRIC_NAMES.put("qps", "QPS_SPIKE");
        METRIC_NAMES.put("errorRate", "ERROR_RATE");
        METRIC_NAMES.put("avgCost", "SLOW_RESPONSE");
        METRIC_NAMES.put("ipConcentration", "IP_BURST");

        SCALE_FLOOR.put("qps", 0.50);               // 每秒请求数，波动半次以内视为噪声
        SCALE_FLOOR.put("errorRate", 0.05);         // 错误率，5 个百分点以内视为噪声
        SCALE_FLOOR.put("avgCost", 10.0);           // 毫秒，10ms 以内视为噪声
        SCALE_FLOOR.put("ipConcentration", 0.05);   // 单 IP 占比，5 个百分点以内视为噪声
    }

    private final AccessLogMapper accessLogMapper;
    private final SecurityAlertMapper securityAlertMapper;
    private final IpBlacklistMapper ipBlacklistMapper;

    /** 审计日志异步写入队列 */
    private final LinkedBlockingQueue<AccessLog> logQueue = new LinkedBlockingQueue<>(20000);
    /** IP 黑名单内存缓存：ip -> 到期时间 */
    private final ConcurrentHashMap<String, LocalDateTime> blockCache = new ConcurrentHashMap<>();

    public SecOpsService(AccessLogMapper accessLogMapper,
                         SecurityAlertMapper securityAlertMapper,
                         IpBlacklistMapper ipBlacklistMapper) {
        this.accessLogMapper = accessLogMapper;
        this.securityAlertMapper = securityAlertMapper;
        this.ipBlacklistMapper = ipBlacklistMapper;
    }

    @PostConstruct
    public void init() {
        // 恢复尚未失效的黑名单规则
        try {
            List<IpBlacklist> all = ipBlacklistMapper.selectList(null);
            LocalDateTime now = LocalDateTime.now();
            for (IpBlacklist item : all) {
                if (item.getExpireTime() == null || item.getExpireTime().isAfter(now)) {
                    blockCache.put(item.getIp(), item.getExpireTime());
                }
            }
        } catch (Exception ignored) {
            // 数据库尚未就绪时不影响启动
        }
        Thread worker = new Thread(this::drainQueue, "secops-log-writer");
        worker.setDaemon(true);
        worker.start();
    }

    // ------------------------------------------------------------------
    // 一、访问日志异步写入
    // ------------------------------------------------------------------

    /** 由拦截器调用，只做一次入队，不阻塞业务线程 */
    public void record(AccessLog log) {
        logQueue.offer(log);
    }

    private void drainQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                AccessLog first = logQueue.poll(500, TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue;
                }
                List<AccessLog> batch = new ArrayList<>();
                batch.add(first);
                logQueue.drainTo(batch, 99);
                for (AccessLog log : batch) {
                    try {
                        accessLogMapper.insert(log);
                    } catch (Exception ignored) {
                        // 审计日志写入失败不能影响业务
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ------------------------------------------------------------------
    // 二、IP 黑名单
    // ------------------------------------------------------------------

    public boolean isBlocked(String ip) {
        if (ip == null) {
            return false;
        }
        LocalDateTime expire = blockCache.get(ip);
        if (expire == null) {
            return false;
        }
        // null 表示永久封禁
        if (expire.isAfter(LocalDateTime.now())) {
            return true;
        }
        blockCache.remove(ip);
        return false;
    }

    /** 自动封禁（登录接口暴力破解触发） */
    public void autoBlock(String ip, String reason, int minutes) {
        addBlacklist(ip, reason, "AUTO", minutes);
    }

    public IpBlacklist addBlacklist(String ip, String reason, String source, Integer minutes) {
        IpBlacklist item = new IpBlacklist();
        item.setIp(ip);
        item.setReason(reason);
        item.setSource(source == null ? "MANUAL" : source);
        item.setCreateTime(LocalDateTime.now());
        item.setExpireTime(minutes == null ? null : LocalDateTime.now().plusMinutes(minutes));
        ipBlacklistMapper.insert(item);
        blockCache.put(ip, item.getExpireTime() == null
                ? LocalDateTime.now().plusYears(10) : item.getExpireTime());
        return item;
    }

    public void removeBlacklist(Long id) {
        IpBlacklist item = ipBlacklistMapper.selectById(id);
        if (item != null) {
            ipBlacklistMapper.deleteById(id);
            blockCache.remove(item.getIp());
        }
    }

    public List<IpBlacklist> listBlacklist() {
        return ipBlacklistMapper.selectList(new LambdaQueryWrapper<IpBlacklist>()
                .orderByDesc(IpBlacklist::getId).last("limit 200"));
    }

    // ------------------------------------------------------------------
    // 三、流量统计
    // ------------------------------------------------------------------

    private List<AccessLog> recentLogs(int minutes) {
        LocalDateTime from = LocalDateTime.now().minusMinutes(minutes);
        return accessLogMapper.selectList(new LambdaQueryWrapper<AccessLog>()
                .ge(AccessLog::getCreateTime, from)
                .orderByAsc(AccessLog::getCreateTime)
                .last("limit 10000"));
    }

    /** 监控看板总览 */
    public Map<String, Object> overview() {
        List<AccessLog> logs = recentLogs(60 * 24);
        long total = logs.size();
        long errors = logs.stream().filter(l -> l.getStatusCode() != null && l.getStatusCode() >= 400).count();
        long limited = logs.stream().filter(l -> Integer.valueOf(AccessLog.RISK_RATE_LIMIT).equals(l.getRiskFlag())
                || Integer.valueOf(AccessLog.RISK_BLACKLIST).equals(l.getRiskFlag())).count();
        long risky = logs.stream().filter(l -> Integer.valueOf(AccessLog.RISK_PAYLOAD).equals(l.getRiskFlag())).count();
        double avgCost = logs.stream().filter(l -> l.getCostMs() != null)
                .mapToLong(AccessLog::getCostMs).average().orElse(0);
        long distinctIps = logs.stream().filter(l -> l.getClientIp() != null)
                .map(AccessLog::getClientIp).distinct().count();
        Long unhandled = securityAlertMapper.selectCount(new LambdaQueryWrapper<SecurityAlert>()
                .eq(SecurityAlert::getHandled, 0));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalRequests", total);
        data.put("errorRate", percent(errors, total));
        data.put("avgCostMs", round(avgCost));
        data.put("activeIps", distinctIps);
        data.put("blockedHits", limited);
        data.put("suspiciousHits", risky);
        data.put("unhandledAlerts", unhandled == null ? 0 : unhandled);
        data.put("blacklistSize", blockCache.size());

        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("globalLimit", "每 IP 每分钟 120 次请求");
        policy.put("loginLimit", "每 IP 每分钟 8 次登录尝试，超限自动封禁 10 分钟");
        policy.put("payloadFilter", "SQL 注入 / XSS / 路径穿越特征检测");
        policy.put("detectWindow", DETECT_WINDOW_MINUTES + " 分钟滑动窗口，EWMA 动态基线 + 3σ 偏离");
        policy.put("algorithm", "α=0.30，综合评分 ≥ 3.0 触发告警");
        data.put("policy", policy);
        return data;
    }

    /** 按分钟聚合的趋势序列（QPS、平均耗时、错误数） */
    public List<Map<String, Object>> trend(int minutes) {
        List<AccessLog> logs = recentLogs(minutes);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        Map<LocalDateTime, List<AccessLog>> grouped = logs.stream()
                .filter(l -> l.getCreateTime() != null)
                .collect(Collectors.groupingBy(l -> l.getCreateTime().truncatedTo(ChronoUnit.MINUTES)));

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = minutes - 1; i >= 0; i--) {
            LocalDateTime slot = now.minusMinutes(i);
            List<AccessLog> slotLogs = grouped.getOrDefault(slot, Collections.emptyList());
            double avg = slotLogs.stream().filter(l -> l.getCostMs() != null)
                    .mapToLong(AccessLog::getCostMs).average().orElse(0);
            long err = slotLogs.stream().filter(l -> l.getStatusCode() != null && l.getStatusCode() >= 400).count();
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("time", slot.toString().substring(11, 16));
            point.put("qps", round(slotLogs.size() / 60.0));
            point.put("requests", slotLogs.size());
            point.put("avgCostMs", round(avg));
            point.put("errors", err);
            result.add(point);
        }
        return result;
    }

    public List<Map<String, Object>> statusDistribution() {
        List<AccessLog> logs = recentLogs(60 * 24);
        Map<Integer, Long> grouped = logs.stream()
                .filter(l -> l.getStatusCode() != null)
                .collect(Collectors.groupingBy(AccessLog::getStatusCode, Collectors.counting()));
        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("status", String.valueOf(e.getKey()));
                    m.put("count", e.getValue());
                    return m;
                }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> topIps(int limit) {
        List<AccessLog> logs = recentLogs(60 * 24);
        Map<String, Long> grouped = logs.stream()
                .filter(l -> l.getClientIp() != null)
                .collect(Collectors.groupingBy(AccessLog::getClientIp, Collectors.counting()));
        return grouped.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("ip", e.getKey());
                    m.put("count", e.getValue());
                    m.put("blocked", blockCache.containsKey(e.getKey()));
                    return m;
                }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> slowApis(int limit) {
        List<AccessLog> logs = recentLogs(60 * 24);
        Map<String, List<AccessLog>> grouped = logs.stream()
                .filter(l -> l.getUri() != null && l.getCostMs() != null)
                .collect(Collectors.groupingBy(AccessLog::getUri));
        return grouped.entrySet().stream()
                .map(e -> {
                    double avg = e.getValue().stream().mapToLong(AccessLog::getCostMs).average().orElse(0);
                    long max = e.getValue().stream().mapToLong(AccessLog::getCostMs).max().orElse(0);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("uri", e.getKey());
                    m.put("count", e.getValue().size());
                    m.put("avgCostMs", round(avg));
                    m.put("maxCostMs", max);
                    m.put("avg", avg);
                    return m;
                })
                .sorted((a, b) -> Double.compare((Double) b.get("avg"), (Double) a.get("avg")))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<SecurityAlert> listAlerts(int limit) {
        return securityAlertMapper.selectList(new LambdaQueryWrapper<SecurityAlert>()
                .orderByDesc(SecurityAlert::getId).last("limit " + Math.max(1, Math.min(limit, 200))));
    }

    public void handleAlert(Long id) {
        SecurityAlert alert = securityAlertMapper.selectById(id);
        if (alert != null) {
            alert.setHandled(1);
            securityAlertMapper.updateById(alert);
        }
    }

    // ------------------------------------------------------------------
    // 四、基于 EWMA 动态基线的异常检测
    // ------------------------------------------------------------------

    /**
     * 定时检测：每分钟执行一次，读取最近 12 个一分钟窗口的日志，
     * 用指数加权移动平均维护每个指标的均值与方差，计算标准化偏离并生成告警。
     */
    @Scheduled(fixedDelay = 60000L, initialDelay = 30000L)
    public Map<String, Object> detect() {
        return detectNow();
    }

    /** 也可由管理员在看板中手动触发 */
    public Map<String, Object> detectNow() {
        List<AccessLog> logs = recentLogs(DETECT_WINDOW_MINUTES + 1);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        Map<LocalDateTime, List<AccessLog>> grouped = logs.stream()
                .filter(l -> l.getCreateTime() != null)
                .collect(Collectors.groupingBy(l -> l.getCreateTime().truncatedTo(ChronoUnit.MINUTES)));

        // 按时间顺序构造窗口，空窗口按 0 补齐，保证基线连续
        List<Map<String, Double>> windows = new ArrayList<>();
        for (int i = DETECT_WINDOW_MINUTES - 1; i >= 0; i--) {
            List<AccessLog> slotLogs = grouped.getOrDefault(now.minusMinutes(i), Collections.emptyList());
            windows.add(metricsOf(slotLogs));
        }

        Map<String, Object> report = new LinkedHashMap<>();
        Map<String, double[]> baseline = new HashMap<>(); // [均值, 方差]
        for (String key : WEIGHTS.keySet()) {
            baseline.put(key, new double[]{0.0, 0.0});
        }

        int validWindows = 0;
        Map<String, Double> lastZ = new LinkedHashMap<>();
        Map<String, Double> lastValue = new LinkedHashMap<>();
        Map<String, Double> lastBaseline = new LinkedHashMap<>();
        double totalScore = 0.0;

        for (int idx = 0; idx < windows.size(); idx++) {
            Map<String, Double> current = windows.get(idx);
            boolean isLast = (idx == windows.size() - 1);
            if (current.get("requests") > 0) {
                validWindows++;
            }
            for (String key : WEIGHTS.keySet()) {
                double x = current.get(key);
                double[] state = baseline.get(key);
                double mean = state[0];
                double variance = state[1];
                double z = Math.abs(x - mean) / (Math.sqrt(variance) + SCALE_FLOOR.getOrDefault(key, 1.0));
                if (isLast) {
                    lastZ.put(key, round(z));
                    lastValue.put(key, round(x));
                    lastBaseline.put(key, round(mean));
                    totalScore += WEIGHTS.get(key) * Math.min(z, 10.0);
                }
                // 用不含当前点的历史基线更新：先判偏离，再吸收观测值
                double newMean = ALPHA * x + (1 - ALPHA) * mean;
                double newVariance = ALPHA * Math.pow(x - newMean, 2) + (1 - ALPHA) * variance;
                state[0] = newMean;
                state[1] = newVariance;
            }
        }

        boolean warmed = validWindows >= 5;
        report.put("status", warmed ? "DETECTED" : "WARMUP");
        report.put("validWindows", validWindows);
        report.put("score", round(totalScore));
        report.put("threshold", ALERT_THRESHOLD);
        report.put("current", lastValue);
        report.put("baseline", lastBaseline);
        report.put("deviation", lastZ);

        List<String> triggered = new ArrayList<>();
        if (warmed && totalScore >= ALERT_THRESHOLD) {
            String level = totalScore >= 6.0 ? "CRITICAL" : "WARN";
            for (String key : WEIGHTS.keySet()) {
                double z = lastZ.getOrDefault(key, 0.0);
                if (z < 2.0) {
                    continue;
                }
                String alertType = METRIC_NAMES.get(key);
                if (hasRecentAlert(alertType)) {
                    continue;
                }
                SecurityAlert alert = new SecurityAlert();
                alert.setAlertType(alertType);
                alert.setLevel(z >= 4.0 ? "CRITICAL" : level);
                alert.setMetricName(key);
                alert.setMetricValue(bd(lastValue.get(key)));
                alert.setBaselineValue(bd(lastBaseline.get(key)));
                alert.setScore(bd(round(totalScore)));
                alert.setDetail(String.format("指标 %s 当前值 %.2f，动态基线 %.2f，偏离 %.2fσ，综合评分 %.2f",
                        key, lastValue.get(key), lastBaseline.get(key), z, totalScore));
                alert.setHandled(0);
                alert.setCreateTime(LocalDateTime.now());
                securityAlertMapper.insert(alert);
                triggered.add(alertType);
            }
        }
        report.put("triggered", triggered);
        return report;
    }

    private boolean hasRecentAlert(String alertType) {
        Long count = securityAlertMapper.selectCount(new LambdaQueryWrapper<SecurityAlert>()
                .eq(SecurityAlert::getAlertType, alertType)
                .ge(SecurityAlert::getCreateTime, LocalDateTime.now().minusMinutes(5)));
        return count != null && count > 0;
    }

    /** 单个时间窗口的四维指标 */
    private Map<String, Double> metricsOf(List<AccessLog> logs) {
        Map<String, Double> m = new LinkedHashMap<>();
        int count = logs.size();
        double errors = logs.stream().filter(l -> l.getStatusCode() != null && l.getStatusCode() >= 400).count();
        double avgCost = logs.stream().filter(l -> l.getCostMs() != null)
                .mapToLong(AccessLog::getCostMs).average().orElse(0);
        double maxIpCount = logs.stream().filter(l -> l.getClientIp() != null)
                .collect(Collectors.groupingBy(AccessLog::getClientIp, Collectors.counting()))
                .values().stream().mapToLong(Long::longValue).max().orElse(0);
        m.put("requests", (double) count);
        m.put("qps", count / 60.0);
        m.put("errorRate", count == 0 ? 0.0 : errors / count);
        m.put("avgCost", avgCost);
        m.put("ipConcentration", count == 0 ? 0.0 : maxIpCount / count);
        return m;
    }

    /**
     * 流量回放：生成带时间跨度的历史访问日志，用于验证检测算法在
     * 突发流量、错误率上升、响应变慢与单 IP 集中访问四类异常下的有效性。
     * 正常基线覆盖前 11 分钟，异常流量注入最近 1 分钟。
     */
    public Map<String, Object> simulateTraffic() {
        Random random = new Random();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        String[] normalIps = {"192.168.10.21", "192.168.10.22", "192.168.10.23",
                "192.168.10.24", "192.168.10.25"};
        String[] uris = {"/api/product/page", "/api/category/list", "/api/order/page",
                "/api/cart/list", "/api/review/product/1", "/api/user/info"};

        int normal = 0;
        for (int i = DETECT_WINDOW_MINUTES - 1; i >= 1; i--) {
            LocalDateTime slot = now.minusMinutes(i);
            int n = 18 + random.nextInt(8);
            for (int j = 0; j < n; j++) {
                AccessLog log = new AccessLog();
                log.setUri(uris[random.nextInt(uris.length)]);
                log.setMethod("GET");
                log.setClientIp(normalIps[random.nextInt(normalIps.length)]);
                log.setStatusCode(200);
                log.setCostMs(20L + random.nextInt(50));
                log.setRiskFlag(AccessLog.RISK_NONE);
                log.setUserAgent("Mozilla/5.0 (replay-normal)");
                log.setCreateTime(slot.plusSeconds(random.nextInt(55)));
                accessLogMapper.insert(log);
                normal++;
            }
        }

        // 异常窗口：单一源 IP 高频请求，伴随大量 401/429 与明显变慢的响应
        int burst = 0;
        LocalDateTime attackSlot = now;
        for (int j = 0; j < 160; j++) {
            AccessLog log = new AccessLog();
            log.setUri("/api/auth/login");
            log.setMethod("POST");
            log.setClientIp("203.0.113.77");
            log.setStatusCode(random.nextInt(100) < 62 ? 429 : 200);
            log.setCostMs(300L + random.nextInt(600));
            log.setRiskFlag(AccessLog.RISK_RATE_LIMIT);
            log.setUserAgent("Mozilla/5.0 (replay-burst)");
            log.setCreateTime(attackSlot.plusSeconds(random.nextInt(55)));
            accessLogMapper.insert(log);
            burst++;
        }

        Map<String, Object> detectReport = detectNow();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("normalLogs", normal);
        result.put("burstLogs", burst);
        result.put("attackIp", "203.0.113.77");
        result.put("detect", detectReport);
        return result;
    }

    // ------------------------------------------------------------------
    // 工具方法
    // ------------------------------------------------------------------

    private double percent(long part, long total) {
        return total == 0 ? 0.0 : round(part * 100.0 / total);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal bd(Double value) {
        return value == null ? BigDecimal.ZERO
                : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
