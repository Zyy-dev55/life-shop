package com.lifeshop.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商家发起提现
 */
@Data
public class WithdrawApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "请输入提现金额")
    @DecimalMin(value = "0.01", message = "提现金额必须大于 0")
    private BigDecimal amount;

    /** 1 银行卡 2 微信 3 支付宝 */
    @NotNull(message = "请选择提现方式")
    private Integer method;

    @NotBlank(message = "请输入收款账号")
    private String accountNo;

    /** 持卡人/收款人姓名（银行卡必填） */
    private String accountName;
}
