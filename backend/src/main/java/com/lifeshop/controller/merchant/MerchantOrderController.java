package com.lifeshop.controller.merchant;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
import com.lifeshop.service.MerchantService;
import com.lifeshop.service.OrderService;
import com.lifeshop.vo.OrderVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 商家后台 - 本店订单管理
 */
@RestController
@RequestMapping("/api/merchant/order")
public class MerchantOrderController {

    private final OrderService orderService;
    private final MerchantService merchantService;

    public MerchantOrderController(OrderService orderService, MerchantService merchantService) {
        this.orderService = orderService;
        this.merchantService = merchantService;
    }

    @GetMapping("/page")
    public Result<PageResult<OrderVO>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(orderService.merchantPage(
                merchantService.currentId(), keyword, status, pageNum, pageSize));
    }

    @GetMapping("/pending-count")
    public Result<Long> pendingCount() {
        return Result.success(orderService.merchantPendingShip(merchantService.currentId()));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.merchantDetail(merchantService.currentId(), id));
    }

    /** 发货（需填写物流单号与快递公司） */
    @PutMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.merchantShip(merchantService.currentId(), id,
                body.get("trackingNo"), body.get("company"));
        return Result.success("已发货", null);
    }
}
