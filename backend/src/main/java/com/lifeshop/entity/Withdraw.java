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
 * 商家提现申请
 */
@Data
@TableName("t_withdraw")
public class Withdraw implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int STATUS_PENDING = 0;   // 待审核
    public static final int STATUS_APPROVED = 1;  // 已打款
    public static final int STATUS_REJECTED = 2;  // 已驳回

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long merchantId;

    /** 店铺名称快照 */
    private String merchantName;

    private BigDecimal amount;

    /** 1 银行卡 2 微信 3 支付宝 */
    private Integer method;

    /** 收款账号（银行卡号 / 微信 / 支付宝账号） */
    private String accountNo;

    /** 持卡人/收款人姓名 */
    private String accountName;

    /** 0 待审核 1 已打款 2 已驳回 */
    private Integer status;

    /** 审核备注（驳回时填写原因） */
    private String auditRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    /** 审核人 */
    private String auditBy;

    /** 前端展示用：提现方式文字 */
    @TableField(exist = false)
    private String methodText;
}
