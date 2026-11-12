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
 * 优惠券模板（平台发放，用户领取后在结算时抵扣）
 */
@Data
@TableName("t_coupon")
public class Coupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 券名称，如「满 99 减 10」 */
    private String name;

    /** 1 满减券 2 无门槛券 */
    private Integer type;

    /** 满减券的使用门槛（满 X 元可用），无门槛券为 0 */
    private BigDecimal threshold;

    /** 抵扣金额 */
    private BigDecimal amount;

    /** 总发行数量 */
    private Integer total;

    /** 已领取数量 */
    private Integer received;

    /** 每人限领张数 */
    private Integer limitPerUser;

    /** 领取后有效天数 */
    private Integer validDays;

    /** 1 上架 0 下架 */
    private Integer status;

    /** 使用说明 */
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 前端展示用：当前用户是否已领取 */
    private transient boolean receivedByMe;
}
