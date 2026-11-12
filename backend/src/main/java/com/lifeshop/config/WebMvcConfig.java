package com.lifeshop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.File;
import java.io.IOException;

/**
 * Web 配置：跨域 + 拦截器 + 上传文件静态映射 + 前端单页应用托管
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 打包后前端产物位于 classpath:/static/index.html */
    private static final Resource INDEX_HTML = new ClassPathResource("static/index.html");

    private final SecurityInterceptor securityInterceptor;
    private final JwtInterceptor jwtInterceptor;
    private final AdminInterceptor adminInterceptor;
    private final MerchantInterceptor merchantInterceptor;

    @Value("${lifeshop.upload.dir:./uploads}")
    private String uploadDir;

    public WebMvcConfig(SecurityInterceptor securityInterceptor,
                        JwtInterceptor jwtInterceptor, AdminInterceptor adminInterceptor,
                        MerchantInterceptor merchantInterceptor) {
        this.securityInterceptor = securityInterceptor;
        this.jwtInterceptor = jwtInterceptor;
        this.adminInterceptor = adminInterceptor;
        this.merchantInterceptor = merchantInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 网络安全拦截器置于最外层：黑名单、限流、载荷过滤、安全响应头、访问审计
        // 先于身份鉴权执行，未登录的攻击流量同样会留痕
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/error");

        // 需要登录的接口
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/product/**",
                        "/api/category/**",
                        "/api/banner/**",
                        "/api/review/product/**",
                        "/error");

        // 需要管理员权限的接口
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**");

        // 需要商家权限的接口
        registry.addInterceptor(merchantInterceptor)
                .addPathPatterns("/api/merchant/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /** 上传的图片按 /uploads/** 直接访问；其余路径交给打包进来的前端 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File dir = new File(uploadDir);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + dir.getAbsolutePath() + File.separator);

        // 前端构建产物放在 classpath:/static/ 下，由后端单端口托管。
        // Vue 用的是 history 模式，直接访问 /product/1 这类地址时磁盘上没有对应文件，
        // 这里回退到 index.html，交给前端路由处理。
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // 根路径直接给首页
                        if (resourcePath.isEmpty() || "/".equals(resourcePath)) {
                            return INDEX_HTML.exists() ? INDEX_HTML : null;
                        }
                        Resource resource = location.createRelative(resourcePath);
                        if (resource.exists() && resource.isReadable()) {
                            return resource;
                        }
                        // 接口、上传目录、H2 控制台、错误页不走前端回退
                        if (isBackendPath(resourcePath) || resourcePath.contains(".")) {
                            return null;
                        }
                        return INDEX_HTML.exists() ? INDEX_HTML : null;
                    }
                });
    }

    private boolean isBackendPath(String path) {
        return path.startsWith("api/")
                || path.startsWith("uploads/")
                || path.startsWith("h2-console")
                || path.startsWith("error");
    }
}
