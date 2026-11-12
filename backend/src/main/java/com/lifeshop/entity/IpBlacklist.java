package com.lifeshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * IP 黑名单。支持登录防爆破自动封禁（AUTO）与管理员手工封禁（MANUAL），
 * 到期后自动失效，避免永久占用规则表。
 */
@Data
@TableName("t_ip_blacklist")
public class IpBlacklist implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ip;

    private String reason;

    /** AUTO / MANUAL */
    private String source;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
