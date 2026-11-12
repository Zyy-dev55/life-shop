package com.lifeshop.dto;

import lombok.Data;

/**
 * 买家退货填写物流信息
 */
@Data
public class AfterSaleReturnDTO {

    private String trackingNo;

    private String company;
}
