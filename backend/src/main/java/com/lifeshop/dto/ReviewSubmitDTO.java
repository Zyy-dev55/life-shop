package com.lifeshop.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 提交评价
 */
@Data
public class ReviewSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "订单不能为空")
    private Long orderId;

    @NotNull(message = "商品不能为空")
    private Long productId;

    @NotNull(message = "请选择评分")
    @Min(value = 1, message = "评分最低 1 星")
    @Max(value = 5, message = "评分最高 5 星")
    private Integer rating;

    @Size(max = 500, message = "评价内容最多 500 字")
    private String content;

    /** 评价图片地址（先调 /api/upload 上传） */
    private List<String> images;

    /** 1 匿名评价 */
    private Integer anonymous;
}
