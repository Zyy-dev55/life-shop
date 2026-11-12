package com.lifeshop.controller;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
import com.lifeshop.common.UserContext;
import com.lifeshop.dto.AfterSaleApplyDTO;
import com.lifeshop.dto.AfterSaleReturnDTO;
import com.lifeshop.entity.AfterSale;
import com.lifeshop.service.AfterSaleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户售后接口：申请、我的售后、撤销、退货
 */
@RestController
@RequestMapping("/api/after-sale")
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    public AfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    /** 申请售后 */
    @PostMapping
    public Result<AfterSale> apply(@RequestBody AfterSaleApplyDTO dto) {
        return Result.success("申请成功", afterSaleService.apply(UserContext.getUserId(), dto));
    }

    /** 我的售后列表 */
    @GetMapping("/my")
    public Result<PageResult<AfterSale>> my(@RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(afterSaleService.pageForUser(UserContext.getUserId(), status, pageNum, pageSize));
    }

    /** 售后详情 */
    @GetMapping("/{id}")
    public Result<AfterSale> detail(@PathVariable Long id) {
        return Result.success(afterSaleService.detailForUser(UserContext.getUserId(), id));
    }

    /** 撤销申请 */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        afterSaleService.cancel(UserContext.getUserId(), id);
        return Result.success("已撤销", null);
    }

    /** 填写退货物流 */
    @PutMapping("/{id}/return")
    public Result<Void> returnGoods(@PathVariable Long id, @RequestBody AfterSaleReturnDTO dto) {
        afterSaleService.returnGoods(UserContext.getUserId(), id, dto);
        return Result.success("已提交退货物流", null);
    }
}
