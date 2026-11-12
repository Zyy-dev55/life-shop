package com.lifeshop.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 待评价的订单商品（已完成订单中还没评价的明细）
 */
@Data
public class PendingReviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String orderNo;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer quantity;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;
}
