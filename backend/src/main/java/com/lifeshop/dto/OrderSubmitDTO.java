package com.lifeshop.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class OrderSubmitDTO implements Serializable {

    @NotNull(message = "请选择收货地址")
    private Long addressId;

    private String remark;

    /** 选用的优惠券（用户已领取的券 id），整单抵扣 */
    private Long userCouponId;

    /** 为空则结算购物车中已勾选的商品；不为空则为「立即购买」 */
    private List<OrderItemDTO> items;
}
