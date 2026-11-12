package com.lifeshop.dto;

import lombok.Data;

/**
 * 商家处理售后（同意 / 拒绝）
 */
@Data
public class AfterSaleHandleDTO {

    /** 1 同意 2 拒绝 */
    private Integer action;

    /** 处理备注 */
    private String remark;
}
