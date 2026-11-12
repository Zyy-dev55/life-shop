package com.lifeshop.unit;

import com.lifeshop.config.SecurityInterceptor;
import com.lifeshop.entity.AccessLog;
import com.lifeshop.service.SecOpsService;
import com.lifeshop.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L2 网络安全拦截层回归：源 IP 识别、黑名单、滑动窗口限流、登录防爆破、
 * 攻击载荷过滤、安全响应头与访问审计。
 *
 * <p>这一层位于鉴权之前，所以断言重点是「HTTP 状态码」与「审计留痕」，
 * 而不是业务返回体——攻击流量根本不应该到达 Controller。
 */
@DisplayName("L2 安全拦截器：限流 / 过滤 / 审计 / 响应头")
class SecurityInterceptorTest {

    private SecOpsService secOpsService;
    private SecurityInterceptor interceptor;

    @BeforeEach
    void setUp() {
        secOpsService = mock(SecOpsService.class);
        interceptor = new SecurityInterceptor(secOpsService, mock(JwtUtil.class));
        ReflectionTestUtils.setField(interceptor, "globalLimit", 120);
        ReflectionTestUtils.setField(interceptor, "loginLimit", 8);
        ReflectionTestUtils.setField(interceptor, "blockMinutes", 10);
    }

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        req.setRemoteAddr("127.0.0.1");
        return req;
    }

    @Test
    @DisplayName("正常请求放行，并下发完整的安全响应头")
    void shouldPassCleanRequestAndSetSecurityHeaders() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/product/page");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(req, resp, new Object())).isTrue();

        assertThat(resp.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(resp.getHeader("X-Frame-Options")).isEqualTo("SAMEORIGIN");
        assertThat(resp.getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
        assertThat(resp.getHeader("Content-Security-Policy-Report-Only"))
                .contains("default-src 'self'").contains("frame-ancestors 'self'");
        // 断言：放行前未误触发任何封禁或限流
        verify(secOpsService, never()).autoBlock(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("XSS / SQL 注入 / 路径穿越载荷一律 403，且写入攻击审计标记")
    void shouldBlockSuspiciousPayloads() throws Exception {
        String[] attacks = {
                "q=<script>alert(1)</script>",
                "id=1' or 1=1--",
                "q=1 UNION ALL SELECT password FROM t_user",
                "file=../../etc/passwd"
        };
        for (String query : attacks) {
            SecOpsService svc = mock(SecOpsService.class);
            SecurityInterceptor isolated = new SecurityInterceptor(svc, mock(JwtUtil.class));
            ReflectionTestUtils.setField(isolated, "globalLimit", 120);
            ReflectionTestUtils.setField(isolated, "loginLimit", 8);
            ReflectionTestUtils.setField(isolated, "blockMinutes", 10);

            MockHttpServletRequest req = request("GET", "/api/product/page");
            req.setQueryString(query);
            MockHttpServletResponse resp = new MockHttpServletResponse();

            assertThat(isolated.preHandle(req, resp, new Object()))
                    .as("载荷应被拦截: %s", query).isFalse();
            assertThat(resp.getStatus()).as("载荷应返回 403: %s", query).isEqualTo(403);
            assertThat(resp.getContentAsString()).contains("安全策略拦截");

            ArgumentCaptor<AccessLog> captor = ArgumentCaptor.forClass(AccessLog.class);
            verify(svc).record(captor.capture());
            assertThat(captor.getValue().getRiskFlag()).isEqualTo(AccessLog.RISK_PAYLOAD);
            assertThat(captor.getValue().getStatusCode()).isEqualTo(403);
        }
    }

    @Test
    @DisplayName("反向代理场景：X-Forwarded-For 首段作为真实源 IP")
    void shouldResolveClientIpFromForwardedHeader() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/product/page");
        req.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        interceptor.preHandle(req, resp, new Object());

        verify(secOpsService).isBlocked("203.0.113.9");
        verify(secOpsService, never()).isBlocked("10.0.0.1");
    }

    @Test
    @DisplayName("命中黑名单 → 403，且不进入后续鉴权与业务链路")
    void shouldRejectBlockedIp() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/product/page");
        req.addHeader("X-Real-IP", "198.51.100.7");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        when(secOpsService.isBlocked("198.51.100.7")).thenReturn(true);

        assertThat(interceptor.preHandle(req, resp, new Object())).isFalse();
        assertThat(resp.getStatus()).isEqualTo(403);
        assertThat(resp.getContentAsString()).contains("黑名单");

        ArgumentCaptor<AccessLog> captor = ArgumentCaptor.forClass(AccessLog.class);
        verify(secOpsService).record(captor.capture());
        assertThat(captor.getValue().getRiskFlag()).isEqualTo(AccessLog.RISK_BLACKLIST);
        assertThat(captor.getValue().getClientIp()).isEqualTo("198.51.100.7");
    }

    @Test
    @DisplayName("登录接口超过阈值 → 429 并自动封禁源 IP")
    void shouldBlockLoginBruteForce() throws Exception {
        ReflectionTestUtils.setField(interceptor, "loginLimit", 3);
        MockHttpServletResponse last = null;
        boolean lastResult = true;
        for (int i = 0; i < 4; i++) {
            MockHttpServletRequest req = request("POST", "/api/auth/login");
            last = new MockHttpServletResponse();
            lastResult = interceptor.preHandle(req, last, new Object());
        }

        assertThat(lastResult).isFalse();
        assertThat(last.getStatus()).isEqualTo(429);
        assertThat(last.getContentAsString()).contains("临时封禁");
        verify(secOpsService).autoBlock(eq("127.0.0.1"), anyString(), eq(10));
    }

    @Test
    @DisplayName("OPTIONS 预检请求直接放行，不消耗限流额度")
    void shouldSkipPreflight() throws Exception {
        MockHttpServletRequest req = request("OPTIONS", "/api/order/page");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(req, resp, new Object())).isTrue();
        verify(secOpsService, never()).isBlocked(anyString());
        verify(secOpsService, never()).record(any());
    }

    @Test
    @DisplayName("afterCompletion 采集访问审计：状态码 / URI / 耗时 / 源 IP 齐全")
    void shouldRecordAccessLogOnCompletion() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/cart/list");
        req.addHeader("User-Agent", "regression-test");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(req, resp, new Object())).isTrue();
        resp.setStatus(200);
        interceptor.afterCompletion(req, resp, new Object(), null);

        ArgumentCaptor<AccessLog> captor = ArgumentCaptor.forClass(AccessLog.class);
        verify(secOpsService).record(captor.capture());
        AccessLog log = captor.getValue();
        assertThat(log.getUri()).isEqualTo("/api/cart/list");
        assertThat(log.getMethod()).isEqualTo("GET");
        assertThat(log.getStatusCode()).isEqualTo(200);
        assertThat(log.getClientIp()).isEqualTo("127.0.0.1");
        assertThat(log.getRiskFlag()).isEqualTo(AccessLog.RISK_NONE);
        assertThat(log.getCostMs()).isNotNull().isGreaterThanOrEqualTo(0L);
        assertThat(log.getCreateTime()).isNotNull();
        assertThat(log.getUserAgent()).isEqualTo("regression-test");
    }
}
