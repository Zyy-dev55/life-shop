package com.lifeshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifeshop.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
