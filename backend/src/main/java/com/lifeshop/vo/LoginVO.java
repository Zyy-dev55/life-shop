package com.lifeshop.vo;

import com.lifeshop.entity.Merchant;
import com.lifeshop.entity.User;
import lombok.Data;

import java.io.Serializable;

@Data
public class LoginVO implements Serializable {

    private String token;

    private User user;

    /** 商家账号登录时带上店铺信息，前端据此进入商家后台 */
    private Merchant merchant;

    public LoginVO() {
    }

    public LoginVO(String token, User user) {
        this.token = token;
        this.user = user;
    }
}
