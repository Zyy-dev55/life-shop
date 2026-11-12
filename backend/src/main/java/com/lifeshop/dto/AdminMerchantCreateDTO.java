package com.lifeshop.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 管理员后台新增商家（同时创建商家账号 + 店铺）
 */
@Data
public class AdminMerchantCreateDTO implements Serializable {

    @NotBlank(message = "请输入商家账号")
    @Size(min = 3, max = 20, message = "账号长度需为 3-20 位")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
    private String password;

    @NotBlank(message = "请输入店铺名称")
    private String shopName;

    private String contact;

    private String phone;

    private String address;

    private String description;

    private String logo;
}
