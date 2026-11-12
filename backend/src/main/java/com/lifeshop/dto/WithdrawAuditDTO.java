package com.lifeshop.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 管理员审核提现
 */
@Data
public class WithdrawAuditDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 1 通过 2 驳回 */
    @NotNull(message = "请选择审核结果")
    private Integer status;

    /** 驳回原因（驳回时必填） */
    private String remark;
}
