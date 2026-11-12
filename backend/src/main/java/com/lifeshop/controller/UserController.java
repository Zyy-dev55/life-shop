package com.lifeshop.controller;

import com.lifeshop.common.Result;
import com.lifeshop.common.UserContext;
import com.lifeshop.dto.PasswordDTO;
import com.lifeshop.dto.UserUpdateDTO;
import com.lifeshop.entity.User;
import com.lifeshop.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 当前登录用户接口
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/info")
    public Result<User> info() {
        return Result.success(userService.getById(UserContext.getUserId()));
    }

    @PutMapping("/info")
    public Result<User> updateInfo(@RequestBody UserUpdateDTO dto) {
        return Result.success("保存成功", userService.updateInfo(UserContext.getUserId(), dto));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody @Valid PasswordDTO dto) {
        userService.changePassword(UserContext.getUserId(), dto);
        return Result.success("密码修改成功", null);
    }
}
