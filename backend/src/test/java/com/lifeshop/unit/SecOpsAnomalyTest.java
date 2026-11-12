package com.lifeshop.unit;

import com.lifeshop.entity.AccessLog;
import com.lifeshop.entity.SecurityAlert;
import com.lifeshop.mapper.AccessLogMapper;
import com.lifeshop.mapper.IpBlacklistMapper;
import com.lifeshop.mapper.SecurityAlertMapper;
import com.lifeshop.service.SecOpsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L2/L6 智能运维回归：EWMA 动态基线 + z-score 加权异常评分。
 *
 * <p>检测算法没有外部依赖，但结果容易被「量纲尺度」「预热窗口数」「告警去重」
 * 三个细节悄悄改变。这里用可复现的合成流量把它钉住：
 * 平稳流量不得告警，突发流量必须命中四类告警，样本不足时必须处于预热态。
 */
@DisplayName("L6 运维检测：动态基线 / 异常评分 / 告警")
class SecOpsAnomalyTest {

    private AccessLogMapper accessLogMapper;
    private SecurityAlertMapper securityAlertMapper;
    private SecOpsService service;

    @BeforeEach
    void setUp() {
        accessLogMapper = mock(AccessLogMapper.class);
        securityAlertMapper = mock(SecurityAlertMapper.class);
        IpBlacklistMapper ipBlacklistMapper = mock(IpBlacklistMapper.class);
        service = new SecOpsService(accessLogMapper, securityAlertMapper, ipBlacklistMapper);
        when(securityAlertMapper.selectCount(any())).thenReturn(0L);
    }

    private AccessLog log(LocalDateTime time, String ip, int status, long cost, int riskFlag, String uri) {
        AccessLog l = new AccessLog();
        l.setUri(uri);
        l.setMethod("GET");
        l.setClientIp(ip);
        l.setStatusCode(status);
        l.setCostMs(cost);
        l.setRiskFlag(riskFlag);
        l.setUserAgent("regression-replay");
        l.setCreateTime(time);
        return l;
    }

    /** 11 分钟平稳基线 + 最近 1 分钟突发流量（与 simulateTraffic 的构造保持一致） */
    private List<AccessLog> burstTraffic() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        String[] normalIps = {"192.168.10.21", "192.168.10.22", "192.168.10.23",
                "192.168.10.24", "192.168.10.25"};
        List<AccessLog> logs = new ArrayList<>();
        for (int i = 11; i >= 1; i--) {
            for (int j = 0; j < 20; j++) {
                logs.add(log(now.minusMinutes(i).plusSeconds(1 + j % 50),
                        normalIps[j % normalIps.length], 200, 45L,
                        AccessLog.RISK_NONE, "/api/product/page"));
            }
        }
        for (int j = 0; j < 160; j++) {
            logs.add(log(now.plusSeconds(1 + j % 50), "203.0.113.77",
                    j % 5 == 0 ? 200 : 429, 600L, AccessLog.RISK_RATE_LIMIT, "/api/auth/login"));
        }
        return logs;
    }

    private List<AccessLog> steadyTraffic() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        String[] ips = {"192.168.10.21", "192.168.10.22", "192.168.10.23",
                "192.168.10.24", "192.168.10.25"};
        List<AccessLog> logs = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            for (int j = 0; j < 20; j++) {
                logs.add(log(now.minusMinutes(i).plusSeconds(1 + j % 50),
                        ips[j % ips.length], 200, 45L, AccessLog.RISK_NONE, "/api/product/page"));
            }
        }
        return logs;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> doubles(Map<String, Object> report, String key) {
        return (Map<String, Double>) report.get(key);
    }

    @Test
    @DisplayName("突发流量：评分超过阈值，四类异常全部生成告警")
    void shouldDetectBurstAndRaiseAlerts() {
        when(accessLogMapper.selectList(any())).thenReturn(burstTraffic());

        Map<String, Object> report = service.detectNow();

        assertThat(report.get("status")).isEqualTo("DETECTED");
        assertThat((Double) report.get("score")).isGreaterThanOrEqualTo(3.0);
        assertThat(report.get("threshold")).isEqualTo(3.0);
        assertThat((Integer) report.get("validWindows")).isGreaterThanOrEqualTo(5);

        // 断言：四个维度的观测值都明显高于各自基线
        assertThat(doubles(report, "current").get("qps"))
                .isGreaterThan(doubles(report, "baseline").get("qps"));
        assertThat(doubles(report, "current").get("errorRate"))
                .isGreaterThan(doubles(report, "baseline").get("errorRate"));
        assertThat(doubles(report, "deviation").get("errorRate")).isGreaterThan(2.0);

        List<String> triggered = (List<String>) report.get("triggered");
        assertThat(triggered).contains("QPS_SPIKE", "ERROR_RATE", "SLOW_RESPONSE", "IP_BURST");
        verify(securityAlertMapper, atLeastOnce()).insert(any(SecurityAlert.class));
    }

    @Test
    @DisplayName("平稳流量：评分低于阈值，不得产生误报")
    void shouldNotAlertOnSteadyTraffic() {
        when(accessLogMapper.selectList(any())).thenReturn(steadyTraffic());

        Map<String, Object> report = service.detectNow();

        assertThat(report.get("status")).isEqualTo("DETECTED"); // 窗口已预热
        assertThat((Double) report.get("score")).isLessThan(3.0);
        assertThat((List<?>) report.get("triggered")).isEmpty();
        verify(securityAlertMapper, never()).insert(any());
    }

    @Test
    @DisplayName("有效窗口不足 5 个 → 保持 WARMUP，不误告警")
    void shouldStayInWarmupWithSparseData() {
        List<AccessLog> sparse = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        for (int i = 0; i < 5; i++) {
            sparse.add(log(now.minusMinutes(1).plusSeconds(i), "203.0.113.77", 429, 900L,
                    AccessLog.RISK_RATE_LIMIT, "/api/auth/login"));
        }
        when(accessLogMapper.selectList(any())).thenReturn(sparse);

        Map<String, Object> report = service.detectNow();

        assertThat(report.get("status")).isEqualTo("WARMUP");
        assertThat((Integer) report.get("validWindows")).isLessThan(5);
        assertThat((List<?>) report.get("triggered")).isEmpty();
        verify(securityAlertMapper, never()).insert(any());
    }

    @Test
    @DisplayName("监控总览：请求量 / 错误率 / 活跃 IP / 策略文案统计正确")
    void shouldAggregateOverviewCorrectly() {
        List<AccessLog> logs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 10; i++) {
            logs.add(log(now.minusSeconds(i), "10.0.0." + (i % 5),
                    i < 2 ? 429 : 200, 20L + i,
                    i == 0 ? AccessLog.RISK_PAYLOAD : AccessLog.RISK_NONE, "/api/cart/list"));
        }
        when(accessLogMapper.selectList(any())).thenReturn(logs);

        Map<String, Object> overview = service.overview();

        assertThat(overview.get("totalRequests")).isEqualTo(10L);
        assertThat(overview.get("errorRate")).isEqualTo(20.0);
        assertThat(overview.get("activeIps")).isEqualTo(5L);
        assertThat(overview.get("suspiciousHits")).isEqualTo(1L);
        assertThat(overview.get("avgCostMs")).isEqualTo(24.5);
        assertThat(overview.get("unhandledAlerts")).isEqualTo(0L);
        @SuppressWarnings("unchecked")
        Map<String, Object> policy = (Map<String, Object>) overview.get("policy");
        assertThat(policy)
                .containsKeys("globalLimit", "loginLimit", "payloadFilter", "detectWindow", "algorithm");
    }

    @Test
    @DisplayName("趋势序列：固定窗口数量，且每个点带齐四个指标")
    void shouldReturnFixedLengthTrend() {
        when(accessLogMapper.selectList(any())).thenReturn(steadyTraffic());

        List<Map<String, Object>> trend = service.trend(5);

        assertThat(trend).hasSize(5);
        assertThat(trend).allSatisfy(point ->
                assertThat(point).containsKeys("time", "qps", "requests", "avgCostMs", "errors"));
    }
}
