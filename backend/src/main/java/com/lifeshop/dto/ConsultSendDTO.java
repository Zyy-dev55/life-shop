package com.lifeshop.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 买家发起咨询
 */
@Data
public class ConsultSendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 咨询哪家店铺 */
    private Long merchantId;

    /** 从哪个商品进来的（可空） */
    private Long productId;

    private String content;
}
