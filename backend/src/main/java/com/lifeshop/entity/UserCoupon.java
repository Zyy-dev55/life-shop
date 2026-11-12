package com.lifeshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户已领取的优惠券
 */
@Data
@TableName("t_user_coupon")
public class UserCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long couponId;

    /** 券名称快照 */
    private String name;

    /** 1 满减券 2 无门槛券 */
    private Integer type;

    private BigDecimal threshold;

    private BigDecimal amount;

    /** 0 未使用 1 已使用 2 已过期 */
    private Integer status;

    /** 领取时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receiveTime;

    /** 有效期截止 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    /** 使用时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime useTime;

    /** 使用的订单号 */
    private String orderNo;
}
