package com.lifeshop.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 商品评价统计
 */
@Data
public class ReviewStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 评价总数 */
    private long total;

    /** 平均分 */
    private BigDecimal avgRating = BigDecimal.ZERO;

    /** 好评率（4~5 星占比，百分比整数） */
    private int goodRate;

    /** 带图评价数 */
    private long withImage;

    /** 各星级数量：5/4/3/2/1 */
    private Map<Integer, Long> starCount = new HashMap<>();
}
