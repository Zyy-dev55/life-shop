package com.lifeshop.controller.admin;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
import com.lifeshop.entity.AfterSale;
import com.lifeshop.service.AfterSaleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员售后监管接口（全量查看）
 */
@RestController
@RequestMapping("/api/admin/after-sale")
public class AdminAfterSaleController {

    private final AfterSaleService afterSaleService;

    public AdminAfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    /** 售后全量列表 */
    @GetMapping("/page")
    public Result<PageResult<AfterSale>> page(@RequestParam(required = false) Integer status,
                                              @RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(afterSaleService.pageForAdmin(status, pageNum, pageSize));
    }
}
