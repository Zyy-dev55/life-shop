package com.lifeshop.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 申请售后
 */
@Data
public class AfterSaleApplyDTO {

    @NotNull(message = "订单项不能为空")
    private Long orderItemId;

    /** 售后类型：1 仅退款 2 退货退款 */
    @NotNull(message = "请选择售后类型")
    private Integer type;

    @NotNull(message = "请选择申请原因")
    private String reason;

    /** 退款金额（可选，默认商品实付金额） */
    private BigDecimal amount;

    private String description;

    /** 凭证图片，逗号分隔 */
    private String images;
}
