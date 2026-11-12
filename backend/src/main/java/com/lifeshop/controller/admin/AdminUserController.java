package com.lifeshop.controller.admin;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
import com.lifeshop.dto.AdminUserCreateDTO;
import com.lifeshop.entity.User;
import com.lifeshop.service.UserService;
import org.springframework.validation.annotation.Validated;
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
 * 后台 - 用户管理
 */
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/page")
    public Result<PageResult<User>> page(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(defaultValue = "1") long pageNum,
                                         @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(userService.page(keyword, status, pageNum, pageSize));
    }

    /** 新增用户 */
    @PostMapping
    public Result<User> create(@RequestBody @Valid AdminUserCreateDTO dto) {
        return Result.success("新增成功", userService.adminCreateUser(dto));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.success("操作成功", null);
    }
}
