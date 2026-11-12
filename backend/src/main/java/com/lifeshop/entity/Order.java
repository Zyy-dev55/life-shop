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
 * 订单
 */
@Data
@TableName("t_order")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单状态常量 */
    public static final int STATUS_UNPAID = 0;
    public static final int STATUS_UNSHIPPED = 1;
    public static final int STATUS_SHIPPED = 2;
    public static final int STATUS_FINISHED = 3;
    public static final int STATUS_CANCELED = 4;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    /** 所属店铺（一次结算混了多个店铺的商品会按店铺拆成多笔订单） */
    private Long merchantId;

    /** 店铺名称快照 */
    private String merchantName;

    private BigDecimal totalAmount;

    private BigDecimal freight;

    /** 优惠券抵扣金额 */
    private BigDecimal discount;

    private BigDecimal payAmount;

    /** 0待付款 1待发货 2待收货 3已完成 4已取消 */
    private Integer status;

    private String receiver;

    private String phone;

    private String address;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shipTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
