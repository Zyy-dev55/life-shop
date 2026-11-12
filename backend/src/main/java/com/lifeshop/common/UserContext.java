package com.lifeshop.common;

/**
 * 当前登录用户上下文（ThreadLocal）
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        return user.getId();
    }

    public static boolean isAdmin() {
        LoginUser user = HOLDER.get();
        return user != null && "ADMIN".equals(user.getRole());
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    public static boolean isMerchant() {
        LoginUser user = HOLDER.get();
        return user != null && "MERCHANT".equals(user.getRole());
    }

    public static void requireMerchant() {
        if (!isMerchant()) {
            throw new BusinessException(403, "仅商家账号可访问");
        }
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 登录用户信息
     */
    public static class LoginUser {
        private Long id;
        private String username;
        private String role;

        public LoginUser() {
        }

        public LoginUser(Long id, String username, String role) {
            this.id = id;
            this.username = username;
            this.role = role;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
