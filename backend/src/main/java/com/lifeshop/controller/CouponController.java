package com.lifeshop.controller;

import com.lifeshop.common.Result;
import com.lifeshop.common.UserContext;
import com.lifeshop.entity.Coupon;
import com.lifeshop.service.CouponService;
import com.lifeshop.vo.UserCouponVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券接口（领券中心公开，领取/我的券/可用券需要登录）
 */
@RestController
@RequestMapping("/api/coupon")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /** 领券中心（未登录也可查看，登录后标记是否已领） */
    @GetMapping("/center")
    public Result<List<Coupon>> center() {
        Long userId = null;
        try {
            userId = UserContext.getUserId();
        } catch (Exception ignored) {
            // 未登录时 userId 为 null，仅展示券列表
        }
        return Result.success(couponService.center(userId));
    }

    /** 领取优惠券 */
    @PostMapping("/{couponId}/receive")
    public Result<Void> receive(@PathVariable Long couponId) {
        couponService.receive(UserContext.getUserId(), couponId);
        return Result.success("领取成功", null);
    }

    /** 我的优惠券 */
    @GetMapping("/my")
    public Result<List<UserCouponVO>> my(@RequestParam(required = false) Integer status) {
        return Result.success(couponService.my(UserContext.getUserId(), status));
    }

    /** 结算可用券（按当前商品总额过滤） */
    @GetMapping("/usable")
    public Result<List<UserCouponVO>> usable(@RequestParam BigDecimal amount) {
        return Result.success(couponService.usable(UserContext.getUserId(), amount));
    }
}
