package com.lifeshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 咨询会话：一个买家在一个店铺只有一条会话（再次咨询复用，更新最近商品）
 */
@Data
@TableName("t_consult")
public class Consult implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 买家昵称快照 */
    private String userName;

    private String userAvatar;

    private Long merchantId;

    /** 店铺名称快照 */
    private String shopName;

    private String shopLogo;

    /** 最近一次咨询的商品（可为空，纯店铺咨询） */
    private Long productId;

    private String productName;

    private String productImage;

    /** 最后一条消息内容，会话列表直接展示 */
    private String lastContent;

    /** 最后一条消息由谁发出：USER / MERCHANT */
    private String lastSender;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastTime;

    /** 买家未读条数 */
    private Integer userUnread;

    /** 商家未读条数 */
    private Integer merchantUnread;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
