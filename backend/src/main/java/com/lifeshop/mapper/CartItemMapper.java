package com.lifeshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifeshop.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}
