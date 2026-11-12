package com.lifeshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品评价（订单完成后，按订单明细逐件评价）
 */
@Data
@TableName("t_review")
public class Review implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 下单时的昵称快照 */
    private String userName;

    private String userAvatar;

    private Long orderId;

    private String orderNo;

    private Long merchantId;

    private Long productId;

    private String productName;

    private String productImage;

    /** 1~5 星 */
    private Integer rating;

    private String content;

    /** 评价图片，英文逗号分隔 */
    private String images;

    /** 1 匿名 0 不匿名 */
    private Integer anonymous;

    /** 商家回复 */
    private String reply;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime replyTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 前台展示用：图片数组 */
    @TableField(exist = false)
    private java.util.List<String> imageList;
}
