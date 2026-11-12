package com.lifeshop.controller.merchant;

import com.lifeshop.common.Result;
import com.lifeshop.entity.Merchant;
import com.lifeshop.service.MerchantService;
import com.lifeshop.vo.MerchantDashboardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家后台 - 店铺资料与经营概览
 */
@RestController
@RequestMapping("/api/merchant")
public class MerchantShopController {

    private final MerchantService merchantService;

    public MerchantShopController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @GetMapping("/profile")
    public Result<Merchant> profile() {
        return Result.success(merchantService.profile());
    }

    @PutMapping("/profile")
    public Result<Merchant> updateProfile(@RequestBody Merchant form) {
        return Result.success("店铺资料已更新", merchantService.updateProfile(form));
    }

    @GetMapping("/dashboard")
    public Result<MerchantDashboardVO> dashboard() {
        return Result.success(merchantService.dashboard());
    }
}
