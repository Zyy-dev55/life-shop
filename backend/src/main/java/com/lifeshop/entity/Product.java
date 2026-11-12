package com.lifeshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品
 */
@Data
@TableName("t_product")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属店铺 */
    private Long merchantId;

    private Long categoryId;

    private String name;

    private String subtitle;

    private String mainImage;

    /** 组图，英文逗号分隔 */
    private String images;

    private String detail;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    private Integer sales;

    /** 平均评分（由评价数据自动维护） */
    private BigDecimal rating;

    /** 评价数 */
    private Integer reviewCount;

    private String unit;

    /** 1上架 0下架 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 分类名称（非数据库字段） */
    @TableField(exist = false)
    private String categoryName;

    /** 店铺名称（非数据库字段） */
    @TableField(exist = false)
    private String merchantName;

    /** 店铺 logo（非数据库字段，咨询弹窗展示用） */
    @TableField(exist = false)
    private String merchantLogo;
}
