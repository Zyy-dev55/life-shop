package com.lifeshop.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 评价展示
 */
@Data
public class ReviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private Long orderId;
    private String orderNo;
    private Long productId;
    private String productName;
    private String productImage;
    private Integer rating;
    private String content;
    private List<String> images = new ArrayList<>();
    private Integer anonymous;
    private String reply;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime replyTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
