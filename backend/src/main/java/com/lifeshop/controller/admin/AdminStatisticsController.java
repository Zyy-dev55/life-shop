package com.lifeshop.controller.admin;

import com.lifeshop.common.Result;
import com.lifeshop.service.StatisticsService;
import com.lifeshop.vo.DashboardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台 - 数据看板
 */
@RestController
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    public AdminStatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.success(statisticsService.dashboard());
    }
}
