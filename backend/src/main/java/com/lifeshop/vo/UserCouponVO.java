package com.lifeshop.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户已领取的优惠券视图（含是否可用于当前金额的判断辅助字段）
 */
@Data
public class UserCouponVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long couponId;
    private String name;
    /** 1 满减券 2 无门槛券 */
    private Integer type;
    private BigDecimal threshold;
    private BigDecimal amount;
    /** 0 未使用 1 已使用 2 已过期 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receiveTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    /** 使用的订单号（已使用时） */
    private String orderNo;

    /** 针对当前结算金额是否可用（前端传入 amount 时计算） */
    private Boolean usable;
}
