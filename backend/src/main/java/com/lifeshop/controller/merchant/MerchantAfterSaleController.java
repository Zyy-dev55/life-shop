package com.lifeshop.controller.merchant;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
import com.lifeshop.dto.AfterSaleHandleDTO;
import com.lifeshop.entity.AfterSale;
import com.lifeshop.service.AfterSaleService;
import com.lifeshop.service.MerchantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家售后处理接口
 */
@RestController
@RequestMapping("/api/merchant/after-sale")
public class MerchantAfterSaleController {

    private final AfterSaleService afterSaleService;
    private final MerchantService merchantService;

    public MerchantAfterSaleController(AfterSaleService afterSaleService, MerchantService merchantService) {
        this.afterSaleService = afterSaleService;
        this.merchantService = merchantService;
    }

    /** 售后列表 */
    @GetMapping("/page")
    public Result<PageResult<AfterSale>> page(@RequestParam(required = false) Integer status,
                                              @RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(afterSaleService.pageForMerchant(merchantService.currentId(), status, pageNum, pageSize));
    }

    /** 处理售后（同意/拒绝） */
    @PutMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody AfterSaleHandleDTO dto) {
        afterSaleService.handleByMerchant(merchantService.currentId(), id, dto);
        return Result.success("处理成功", null);
    }

    /** 确认收货并退款 */
    @PutMapping("/{id}/confirm-refund")
    public Result<Void> confirmRefund(@PathVariable Long id) {
        afterSaleService.confirmRefundByMerchant(merchantService.currentId(), id);
        return Result.success("已退款", null);
    }
}
