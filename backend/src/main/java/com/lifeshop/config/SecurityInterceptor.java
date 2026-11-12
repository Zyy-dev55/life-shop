package com.lifeshop.config;

import com.lifeshop.entity.AccessLog;
import com.lifeshop.service.SecOpsService;
import com.lifeshop.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * 网络安全拦截器，位于拦截器链最外层。
 *
 * <p>把网络层与应用层的安全措施收敛到一条链路上处理：
 * 源 IP 识别（兼容反向代理透传）→ 黑名单访问控制 → 滑动窗口限流 →
 * 登录接口暴力破解防护 → 可疑载荷过滤 → 安全响应头下发 → 访问审计采集。
 * 审计日志只入队，不阻塞业务线程。
 */
@Component
public class SecurityInterceptor implements HandlerInterceptor {

    private static final String ATTR_START = "secops.startTime";

    /** 全局限流：每 IP 每分钟请求数 */
    @Value("${lifeshop.security.global-limit:120}")
    private int globalLimit;
    /** 登录接口限流：每 IP 每分钟尝试次数 */
    @Value("${lifeshop.security.login-limit:8}")
    private int loginLimit;
    /** 触发登录限流后的自动封禁时长（分钟） */
    @Value("${lifeshop.security.block-minutes:10}")
    private int blockMinutes;

    private final SecOpsService secOpsService;
    private final JwtUtil jwtUtil;

    /** 滑动窗口：key -> 请求时间戳队列 */
    private final ConcurrentHashMap<String, ArrayDeque<Long>> windows = new ConcurrentHashMap<>();
    private final AtomicLong lastClean = new AtomicLong(System.currentTimeMillis());

    /** 常见注入与跨站脚本特征，命中即拒绝请求并留痕 */
    private static final Pattern[] SUSPICIOUS_PATTERNS = {
            Pattern.compile("(?i)<\\s*script"),
            Pattern.compile("(?i)javascript\\s*:"),
            Pattern.compile("(?i)\\bon(error|load|click|mouseover)\\s*="),
            Pattern.compile("(?i)union\\s+(all\\s+)?select"),
            Pattern.compile("(?i)\\b(or|and)\\s+['\"]?\\d+['\"]?\\s*=\\s*['\"]?\\d+"),
            Pattern.compile("(?i)(sleep|benchmark|waitfor\\s+delay)\\s*\\("),
            Pattern.compile("\\.\\./|\\.\\.\\\\")
    };

    public SecurityInterceptor(SecOpsService secOpsService, JwtUtil jwtUtil) {
        this.secOpsService = secOpsService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        setSecurityHeaders(response);
        request.setAttribute(ATTR_START, System.currentTimeMillis());

        String ip = resolveClientIp(request);
        String uri = request.getRequestURI();

        // 1. 黑名单访问控制
        if (secOpsService.isBlocked(ip)) {
            writeError(response, 403, "源 IP 已被列入访问黑名单，请稍后再试");
            record(request, response, ip, 403, AccessLog.RISK_BLACKLIST);
            return false;
        }

        // 2. 登录接口独立的低阈值限流 + 自动封禁（暴力破解防护）
        if (uri.contains("/auth/login")) {
            if (!allow("login:" + ip, loginLimit)) {
                secOpsService.autoBlock(ip, "登录接口短时间连续失败，疑似口令暴力破解", blockMinutes);
                writeError(response, 429, "登录尝试过于频繁，源 IP 已被临时封禁");
                record(request, response, ip, 429, AccessLog.RISK_RATE_LIMIT);
                return false;
            }
        }

        // 3. 全局滑动窗口限流
        if (!allow("global:" + ip, globalLimit)) {
            writeError(response, 429, "请求过于频繁，已触发接口限流，请稍后重试");
            record(request, response, ip, 429, AccessLog.RISK_RATE_LIMIT);
            return false;
        }

        // 4. 可疑载荷过滤（SQL 注入 / XSS / 路径穿越）
        if (hasSuspiciousPayload(request)) {
            writeError(response, 403, "请求包含不被允许的字符，已被安全策略拦截");
            record(request, response, ip, 403, AccessLog.RISK_PAYLOAD);
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long start = (Long) request.getAttribute(ATTR_START);
        if (start == null) {
            return;
        }
        record(request, response, resolveClientIp(request), response.getStatus(), AccessLog.RISK_NONE);
    }

    /** 采集一条访问审计记录并投递到异步队列 */
    private void record(HttpServletRequest request, HttpServletResponse response,
                        String ip, int status, int riskFlag) {
        try {
            AccessLog log = new AccessLog();
            log.setUri(request.getRequestURI());
            log.setMethod(request.getMethod());
            log.setClientIp(ip);
            log.setStatusCode(status);
            Long start = (Long) request.getAttribute(ATTR_START);
            log.setCostMs(start == null ? 0L : System.currentTimeMillis() - start);
            log.setRiskFlag(riskFlag);
            String ua = request.getHeader("User-Agent");
            log.setUserAgent(ua == null ? null : (ua.length() > 250 ? ua.substring(0, 250) : ua));
            log.setCreateTime(LocalDateTime.now());
            fillIdentity(request, log);
            secOpsService.record(log);
        } catch (Exception ignored) {
            // 审计采集不能影响业务响应
        }
    }

    /** 从 JWT 中还原调用者身份，仅用于审计留痕，不参与鉴权决策 */
    private void fillIdentity(HttpServletRequest request, AccessLog log) {
        try {
            String token = request.getHeader("Authorization");
            if (token == null || token.trim().isEmpty()) {
                token = request.getHeader("token");
            }
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            if (token == null || token.trim().isEmpty()) {
                return;
            }
            Claims claims = jwtUtil.parse(token);
            if (claims != null) {
                log.setUserId(Long.valueOf(claims.getSubject()));
                log.setRole(claims.get("role", String.class));
            }
        } catch (Exception ignored) {
            // 无效令牌由登录拦截器负责处理，此处仅留空身份
        }
    }

    // ------------------------------------------------------------------
    // 源 IP 与限流
    // ------------------------------------------------------------------

    /**
     * 源 IP 解析：在 Nginx 等反向代理之后部署时，真实客户端地址由
     * X-Forwarded-For 首段透传；直连部署时退回 TCP 对端地址。
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty()) {
            return realIp.trim();
        }
        String remote = request.getRemoteAddr();
        return "0:0:0:0:0:0:0:1".equals(remote) ? "127.0.0.1" : remote;
    }

    /** 固定 60 秒滑动窗口限流 */
    private boolean allow(String key, int limit) {
        long now = System.currentTimeMillis();
        cleanIdleWindows(now);
        ArrayDeque<Long> queue = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() > 60_000L) {
                queue.pollFirst();
            }
            if (queue.size() >= limit) {
                return false;
            }
            queue.addLast(now);
            return true;
        }
    }

    /** 定期回收空闲窗口，避免长跑后内存无限增长 */
    private void cleanIdleWindows(long now) {
        long last = lastClean.get();
        if (now - last < 300_000L || !lastClean.compareAndSet(last, now)) {
            return;
        }
        for (Map.Entry<String, ArrayDeque<Long>> entry : windows.entrySet()) {
            ArrayDeque<Long> q = entry.getValue();
            synchronized (q) {
                while (!q.isEmpty() && now - q.peekFirst() > 60_000L) {
                    q.pollFirst();
                }
                if (q.isEmpty()) {
                    windows.remove(entry.getKey());
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 载荷过滤与安全响应头
    // ------------------------------------------------------------------

    private boolean hasSuspiciousPayload(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getRequestURI());
        if (request.getQueryString() != null) {
            sb.append('?').append(request.getQueryString());
            try {
                sb.append(URLDecoder.decode(request.getQueryString(), StandardCharsets.UTF_8.name()));
            } catch (Exception ignored) {
                // 非法编码由后续特征匹配按原串处理
            }
        }
        String referer = request.getHeader("Referer");
        if (referer != null) {
            sb.append(referer);
        }
        String text = sb.toString();
        for (Pattern pattern : SUSPICIOUS_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 安全响应头。CSP 采用 Report-Only 模式先观察线上资源加载情况，
     * 待确认无内联脚本依赖后再切换为强制策略，避免一上线就阻断前端资源。
     */
    private void setSecurityHeaders(HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("X-XSS-Protection", "0");
        response.setHeader("Content-Security-Policy-Report-Only",
                "default-src 'self'; script-src 'self' 'unsafe-inline'; "
                        + "style-src 'self' 'unsafe-inline'; img-src 'self' data: http: https:; "
                        + "connect-src 'self'; frame-ancestors 'self'");
    }

    private void writeError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}
