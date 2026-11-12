package com.lifeshop.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 购物车项 + 商品信息
 */
@Data
public class CartVO implements Serializable {

    private Long id;

    private Long productId;

    /** 所属店铺，结算时会按店铺拆单 */
    private Long merchantId;

    private String merchantName;

    private String name;

    private String mainImage;

    private String unit;

    private BigDecimal price;

    private Integer stock;

    private Integer quantity;

    private Integer checked;

    private Integer status;

    /** 小计 = 单价 * 数量 */
    private BigDecimal subtotal;
}
