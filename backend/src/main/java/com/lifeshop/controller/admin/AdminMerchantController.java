package com.lifeshop.controller.admin;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
import com.lifeshop.dto.AdminMerchantCreateDTO;
import com.lifeshop.entity.Merchant;
import com.lifeshop.service.MerchantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 后台 - 商家管理（平台管理员管理入驻商家）
 */
@RestController
@RequestMapping("/api/admin/merchant")
public class AdminMerchantController {

    private final MerchantService merchantService;

    public AdminMerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /** 商家分页 */
    @GetMapping("/page")
    public Result<PageResult<Merchant>> page(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(merchantService.adminPage(keyword, status, pageNum, pageSize));
    }

    /** 新增商家（含账号与店铺） */
    @PostMapping
    public Result<Merchant> create(@RequestBody @Valid AdminMerchantCreateDTO dto) {
        return Result.success("新增成功", merchantService.adminCreate(dto));
    }

    /** 启用 / 停业 */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        merchantService.adminUpdateStatus(id, status);
        return Result.success("操作成功", null);
    }
}
