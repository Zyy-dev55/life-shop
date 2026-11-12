package com.lifeshop.controller.merchant;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
import com.lifeshop.dto.WithdrawApplyDTO;
import com.lifeshop.entity.Withdraw;
import com.lifeshop.service.MerchantService;
import com.lifeshop.service.WithdrawService;
import com.lifeshop.vo.WalletVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 商家后台 - 钱包与提现
 */
@RestController
@RequestMapping("/api/merchant/wallet")
public class MerchantWalletController {

    private final WithdrawService withdrawService;
    private final MerchantService merchantService;

    public MerchantWalletController(WithdrawService withdrawService, MerchantService merchantService) {
        this.withdrawService = withdrawService;
        this.merchantService = merchantService;
    }

    /** 钱包概览 */
    @GetMapping
    public Result<WalletVO> wallet() {
        return Result.success(withdrawService.wallet(merchantService.currentId()));
    }

    /** 发起提现 */
    @PostMapping("/withdraw")
    public Result<Void> apply(@RequestBody @Valid WithdrawApplyDTO dto) {
        withdrawService.apply(merchantService.currentId(), dto);
        return Result.success("提现申请已提交，等待审核", null);
    }

    /** 提现记录 */
    @GetMapping("/withdraw/page")
    public Result<PageResult<Withdraw>> page(@RequestParam(required = false) Integer status,
                                             @RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(withdrawService.merchantPage(merchantService.currentId(), status, pageNum, pageSize));
    }
}
