package com.lifeshop.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商家钱包概览
 */
@Data
public class WalletVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 可提现余额（订单已完成入账 - 已提现/审核中） */
    private BigDecimal balance;

    /** 未入账金额（已支付但未完成的订单货款，还在途） */
    private BigDecimal unsettled;

    /** 审核中的提现金额（已冻结） */
    private BigDecimal pendingWithdraw;

    /** 累计已提现金额（含已打款） */
    private BigDecimal totalWithdrawn;
}
