package com.lifeshop.controller.admin;

import com.lifeshop.common.Result;
import com.lifeshop.entity.IpBlacklist;
import com.lifeshop.entity.SecurityAlert;
import com.lifeshop.service.SecOpsService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 平台管理员：网络安全运维监控接口。
 * 路径统一位于 /api/admin 下，由 AdminInterceptor 做角色鉴权。
 */
@RestController
@RequestMapping("/api/admin/secops")
public class AdminSecOpsController {

    private final SecOpsService secOpsService;

    public AdminSecOpsController(SecOpsService secOpsService) {
        this.secOpsService = secOpsService;
    }

    /** 监控看板总览：请求量、错误率、平均时延、活跃 IP、拦截数、告警数与安全策略 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(secOpsService.overview());
    }

    /** 按分钟聚合的流量趋势 */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "30") int minutes) {
        return Result.success(secOpsService.trend(Math.min(Math.max(minutes, 5), 120)));
    }

    /** HTTP 状态码分布 */
    @GetMapping("/status-distribution")
    public Result<List<Map<String, Object>>> statusDistribution() {
        return Result.success(secOpsService.statusDistribution());
    }

    /** 请求量最高的源 IP */
    @GetMapping("/top-ips")
    public Result<List<Map<String, Object>>> topIps(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(secOpsService.topIps(limit));
    }

    /** 平均耗时最高的接口 */
    @GetMapping("/slow-apis")
    public Result<List<Map<String, Object>>> slowApis(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(secOpsService.slowApis(limit));
    }

    /** 异常告警列表 */
    @GetMapping("/alerts")
    public Result<List<SecurityAlert>> alerts(@RequestParam(defaultValue = "30") int limit) {
        return Result.success(secOpsService.listAlerts(limit));
    }

    /** 标记告警已处理 */
    @PutMapping("/alerts/{id}/handle")
    public Result<Void> handleAlert(@PathVariable Long id) {
        secOpsService.handleAlert(id);
        return Result.success();
    }

    /** 立即执行一次动态基线检测，返回当前值、基线、偏离与命中项 */
    @PostMapping("/detect")
    public Result<Map<String, Object>> detect() {
        return Result.success(secOpsService.detectNow());
    }

    /**
     * 流量回放测试：注入正常基线与一段突发异常流量，再执行检测。
     * 用于在网络运维测试中复现攻击场景并验证告警链路。
     */
    @PostMapping("/simulate")
    public Result<Map<String, Object>> simulate() {
        return Result.success(secOpsService.simulateTraffic());
    }

    /** 黑名单列表 */
    @GetMapping("/blacklist")
    public Result<List<IpBlacklist>> blacklist() {
        return Result.success(secOpsService.listBlacklist());
    }

    /** 手工封禁 IP，minutes 为空表示长期封禁 */
    @PostMapping("/blacklist")
    public Result<IpBlacklist> addBlacklist(@RequestBody Map<String, Object> body) {
        String ip = String.valueOf(body.get("ip"));
        if (ip.trim().isEmpty() || "null".equals(ip)) {
            return Result.error("请填写要封禁的 IP 地址");
        }
        String reason = body.get("reason") == null ? "管理员手工封禁" : String.valueOf(body.get("reason"));
        Integer minutes = null;
        if (body.get("minutes") != null && !String.valueOf(body.get("minutes")).isEmpty()) {
            try {
                minutes = Integer.valueOf(String.valueOf(body.get("minutes")));
            } catch (NumberFormatException ignored) {
                // 留空即长期封禁
            }
        }
        return Result.success(secOpsService.addBlacklist(ip.trim(), reason, "MANUAL", minutes));
    }

    /** 解除封禁 */
    @DeleteMapping("/blacklist/{id}")
    public Result<Void> removeBlacklist(@PathVariable Long id) {
        secOpsService.removeBlacklist(id);
        return Result.success();
    }
}
