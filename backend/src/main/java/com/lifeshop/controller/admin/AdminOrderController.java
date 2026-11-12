package com.lifeshop.controller.admin;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
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
 * 后台 - 订单管理
 */
@RestController
@RequestMapping("/api/admin/order")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/page")
    public Result<PageResult<OrderVO>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(orderService.adminPage(keyword, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.adminDetail(id));
    }

    /** 发货（需填写物流单号与快递公司） */
    @PutMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.ship(id, body.get("trackingNo"), body.get("company"));
        return Result.success("发货成功", null);
    }
}
