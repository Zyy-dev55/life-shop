package com.lifeshop.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * L1~L3 集成回归：真实启动 Spring 容器 + H2 演示库，从 HTTP 入口验证
 * 协议状态、安全响应头、安全拦截、身份鉴权与店铺数据边界。
 *
 * <p>与 L4 单元测试的区别：这里断言的是「请求穿过整条拦截器链之后的样子」，
 * 用来发现拦截器顺序、异常处理、序列化配置等只有在容器里才暴露的问题。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@DisplayName("L1~L3 集成：HTTP 协议 / 安全 / 鉴权 / 数据边界")
class ApiRegressionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = root.path("data").path("token").asText();
        assertThat(token).as("登录必须返回 JWT").isNotBlank();
        return token;
    }

    @Test
    @DisplayName("健康检查返回统一响应结构，并携带安全响应头")
    void shouldExposeHealthWithSecurityHeaders() throws Exception {
        mvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("ok"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    @Test
    @DisplayName("公开商品接口可用且返回分页结构")
    void shouldReturnPublicProductPage() throws Exception {
        mvc.perform(get("/api/product/page").param("pageNum", "1").param("pageSize", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.list.length()").value(6))
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    @Test
    @DisplayName("未携带令牌访问受保护接口 → 业务码 401")
    void shouldRejectAnonymousAccess() throws Exception {
        mvc.perform(get("/api/user/info"))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    @DisplayName("登录 → 携带令牌可读取个人信息，用户标识与账号一致")
    void shouldLoginAndReadOwnProfile() throws Exception {
        String token = login("zhangsan", "123456");

        mvc.perform(get("/api/user/info").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("zhangsan"));
    }

    @Test
    @DisplayName("伪造令牌被拦截在鉴权层，不会进入业务服务")
    void shouldRejectForgedToken() throws Exception {
        mvc.perform(get("/api/user/info").header("Authorization", "Bearer forged.token.value"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("错误 HTTP 方法返回协议层 405，而不是被兜底成业务码 500")
    void shouldReturn405ForUnsupportedMethod() throws Exception {
        mvc.perform(post("/api/auth/health").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));
    }

    @Test
    @DisplayName("XSS 载荷在到达 Controller 之前被 403 拦截")
    void shouldBlockXssBeforeController() throws Exception {
        // 用 queryParam 而不是 param：SecurityInterceptor 检查的是原始 query string，
        // MockMvc 的 param() 只填参数表、不生成 query string，两者不能混用。
        mvc.perform(get("/api/product/page").queryParam("keyword", "<script>alert(1)</script>"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("请求包含不被允许的字符，已被安全策略拦截"));
    }

    @Test
    @DisplayName("普通买家访问管理员运维接口 → 403 无权限")
    void shouldGuardAdminEndpoints() throws Exception {
        String token = login("zhangsan", "123456");

        mvc.perform(get("/api/admin/secops/overview").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限访问"));
    }

    @Test
    @DisplayName("商家只能看到本店订单：店铺标识作为数据边界")
    void shouldEnforceMerchantDataBoundary() throws Exception {
        String token = login("shop_daily", "123456");

        mvc.perform(get("/api/merchant/order/page")
                        .param("pageNum", "1").param("pageSize", "50")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray())
                // 断言语义：结果集中不允许出现任何不属于当前店铺(6)的订单
                .andExpect(jsonPath("$.data.list[?(@.merchantId != 6)]").isEmpty());
    }
}
