package com.lifeshop.vo;

import com.lifeshop.entity.Order;
import com.lifeshop.entity.OrderItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单 + 明细
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderVO extends Order {

    private static final long serialVersionUID = 1L;

    private List<OrderItem> items = new ArrayList<>();

    /** 下单用户名（后台列表展示用） */
    private String username;
}
