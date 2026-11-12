package com.lifeshop.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderItemDTO implements Serializable {

    private Long productId;

    private Integer quantity;
}
