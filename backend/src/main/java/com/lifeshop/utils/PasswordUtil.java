package com.lifeshop.utils;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 密码工具：MD5 + 固定盐（如需更高强度可替换为 BCrypt）
 */
public class PasswordUtil {

    private static final String SALT = "LifeShop@2026";

    public static String encrypt(String rawPassword) {
        return DigestUtils.md5DigestAsHex((rawPassword + SALT).getBytes(StandardCharsets.UTF_8));
    }

    public static boolean matches(String rawPassword, String encryptedPassword) {
        return encrypt(rawPassword).equals(encryptedPassword);
    }
}
