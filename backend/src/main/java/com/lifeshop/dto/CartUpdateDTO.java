package com.lifeshop.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class CartUpdateDTO implements Serializable {

    @NotNull(message = "购物车项不能为空")
    private Long id;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity;
}
