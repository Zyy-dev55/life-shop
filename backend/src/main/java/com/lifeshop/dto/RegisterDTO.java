package com.lifeshop.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
public class RegisterDTO implements Serializable {

    @NotBlank(message = "请输入账号")
    @Size(min = 3, max = 20, message = "账号长度需为 3-20 位")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
    private String password;

    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
