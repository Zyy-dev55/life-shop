package com.lifeshop.controller;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
import com.lifeshop.common.UserContext;
import com.lifeshop.dto.OrderSubmitDTO;
import com.lifeshop.entity.Logistics;
import com.lifeshop.service.LogisticsService;
import com.lifeshop.service.OrderService;
import com.lifeshop.vo.OrderVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 订单接口
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;
    private final LogisticsService logisticsService;

    public OrderController(OrderService orderService, LogisticsService logisticsService) {
        this.orderService = orderService;
        this.logisticsService = logisticsService;
    }

    /** 下单（购物车里混了多个店铺的商品时，会按店铺拆成多笔订单） */
    @PostMapping("/submit")
    public Result<List<OrderVO>> submit(@RequestBody @Valid OrderSubmitDTO dto) {
        List<OrderVO> orders = orderService.submit(UserContext.getUserId(), dto);
        String message = orders.size() > 1
                ? "下单成功，已按店铺拆分为 " + orders.size() + " 个订单"
                : "下单成功";
        return Result.success(message, orders);
    }

    @GetMapping("/page")
    public Result<PageResult<OrderVO>> page(@RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "5") long pageSize) {
        return Result.success(orderService.page(UserContext.getUserId(), status, pageNum, pageSize));
    }

    @GetMapping("/counts")
    public Result<Map<String, Long>> counts() {
        return Result.success(orderService.countByStatus(UserContext.getUserId()));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(UserContext.getUserId(), id));
    }

    @PutMapping("/{id}/pay")
    public Result<OrderVO> pay(@PathVariable Long id) {
        return Result.success("支付成功", orderService.pay(UserContext.getUserId(), id));
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(UserContext.getUserId(), id);
        return Result.success("订单已取消", null);
    }

    @PutMapping("/{id}/confirm")
    public Result<OrderVO> confirm(@PathVariable Long id) {
        return Result.success("已确认收货", orderService.confirm(UserContext.getUserId(), id));
    }

    /** 查看订单物流轨迹 */
    @GetMapping("/{id}/logistics")
    public Result<List<Logistics>> logistics(@PathVariable Long id) {
        return Result.success(logisticsService.byOrderForUser(UserContext.getUserId(), id));
    }
}
