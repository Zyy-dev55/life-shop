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
 * 入驻商家（店铺）
 */
@Data
@TableName("t_merchant")
public class Merchant implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 店主账号（t_user.id，role = MERCHANT） */
    private Long userId;

    /** 店铺名称 */
    private String name;

    private String logo;

    private String description;

    /** 联系人 */
    private String contact;

    private String phone;

    private String address;

    /** 1营业 0关店 */
    private Integer status;

    /** 可提现余额（订单完成后货款入账） */
    private BigDecimal balance;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 登录账号（非数据库字段） */
    @TableField(exist = false)
    private String username;

    /** 在售商品数（非数据库字段） */
    @TableField(exist = false)
    private Long productCount;
}
