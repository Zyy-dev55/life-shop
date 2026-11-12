package com.lifeshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后申请（仅退款 / 退货退款）
 */
@Data
@TableName("t_after_sale")
public class AfterSale implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 售后类型 */
    public static final int TYPE_REFUND_ONLY = 1;   // 仅退款
    public static final int TYPE_RETURN_REFUND = 2; // 退货退款

    /** 售后状态 */
    public static final int STATUS_WAIT_MERCHANT = 0;  // 待商家处理
    public static final int STATUS_WAIT_RETURN = 1;     // 商家同意退货，待买家寄回
    public static final int STATUS_WAIT_RECEIVE = 2;    // 买家已寄回，待商家收货
    public static final int STATUS_REFUNDED = 3;        // 退款完成
    public static final int STATUS_REJECTED = 4;        // 已拒绝
    public static final int STATUS_CANCELED = 5;        // 买家已撤销

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long orderId;

    private String orderNo;

    private Long orderItemId;

    private Long merchantId;

    private String merchantName;

    private Long productId;

    private String productName;

    private String productImage;

    private BigDecimal price;

    private Integer quantity;

    /** 申请退款金额 */
    private BigDecimal amount;

    /** 售后类型：1 仅退款 2 退货退款 */
    private Integer type;

    /** 申请原因 */
    private String reason;

    /** 问题描述 */
    private String description;

    /** 买家上传凭证图片（逗号分隔） */
    private String images;

    /** 售后状态：0待商家处理 1待买家寄回 2待商家收货 3退款完成 4已拒绝 5已撤销 */
    private Integer status;

    /** 退货物流单号 */
    private String returnTrackingNo;

    /** 退货快递公司 */
    private String returnCompany;

    /** 商家处理备注 */
    private String merchantRemark;

    /** 退款方式/打款备注 */
    private String refundRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime merchantHandleTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime returnTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime refundTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 前端展示用：类型文字 */
    @TableField(exist = false)
    private String typeText;

    /** 前端展示用：状态文字 */
    @TableField(exist = false)
    private String statusText;
}
