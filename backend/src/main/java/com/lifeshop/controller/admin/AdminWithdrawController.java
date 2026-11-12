package com.lifeshop.controller.admin;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
import com.lifeshop.dto.WithdrawAuditDTO;
import com.lifeshop.entity.Withdraw;
import com.lifeshop.service.WithdrawService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 后台 - 提现审核
 */
@RestController
@RequestMapping("/api/admin/withdraw")
public class AdminWithdrawController {

    private final WithdrawService withdrawService;

    public AdminWithdrawController(WithdrawService withdrawService) {
        this.withdrawService = withdrawService;
    }

    /** 提现审核列表 */
    @GetMapping("/page")
    public Result<PageResult<Withdraw>> page(@RequestParam(required = false) Integer status,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(withdrawService.adminPage(status, keyword, pageNum, pageSize));
    }

    /** 审核通过 / 驳回 */
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody @Valid WithdrawAuditDTO dto) {
        withdrawService.audit(id, dto);
        return Result.success(dto.getStatus() == Withdraw.STATUS_APPROVED ? "已通过并打款" : "已驳回", null);
    }
}
