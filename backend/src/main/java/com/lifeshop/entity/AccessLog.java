package com.lifeshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 网络访问审计日志。
 * 每一次进入服务端的 HTTP 请求都会由 SecurityInterceptor 采集一条记录，
 * 作为流量统计、异常检测与安全溯源的数据来源。
 */
@Data
@TableName("t_access_log")
public class AccessLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 正常请求 */
    public static final int RISK_NONE = 0;
    /** 命中限流 */
    public static final int RISK_RATE_LIMIT = 1;
    /** 命中 IP 黑名单 */
    public static final int RISK_BLACKLIST = 2;
    /** 命中可疑攻击载荷 */
    public static final int RISK_PAYLOAD = 3;
    /** 登录失败 */
    public static final int RISK_LOGIN_FAILED = 4;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 请求路径（不含查询串，避免记录敏感参数） */
    private String uri;

    private String method;

    /** 客户端源 IP，优先取反向代理透传的 X-Forwarded-For 首段 */
    private String clientIp;

    private Long userId;

    private String role;

    private Integer statusCode;

    /** 接口处理耗时，单位毫秒 */
    private Long costMs;

    private String userAgent;

    private Integer riskFlag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
