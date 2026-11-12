package com.lifeshop.controller;

import com.lifeshop.common.Result;
import com.lifeshop.dto.LoginDTO;
import com.lifeshop.dto.RegisterDTO;
import com.lifeshop.service.UserService;
import com.lifeshop.vo.LoginVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody @Valid RegisterDTO dto) {
        return Result.success("注册成功", userService.register(dto));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.success("登录成功", userService.login(dto));
    }

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("ok");
    }
}
