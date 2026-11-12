package com.lifeshop.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 商家后台 - 店铺经营概览
 */
@Data
public class MerchantDashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long merchantId;
    private String shopName;
    private String contact;

    /** 商品总数 / 在售 / 库存预警 */
    private long productCount;
    private long onSaleCount;
    private long lowStockCount;

    /** 累计订单 / 待发货 */
    private long orderCount;
    private long pendingShipCount;

    /** 今日订单 / 今日销售额（已支付） */
    private long todayOrderCount;
    private BigDecimal todaySales = BigDecimal.ZERO;

    /** 累计销售额（已支付订单） */
    private BigDecimal totalSales = BigDecimal.ZERO;

    /** 待回复评价数 */
    private long unrepliedCount;

    /** 待回复的客户咨询（未读消息条数） */
    private long consultUnread;

    /** 平均评分 */
    private BigDecimal avgRating = BigDecimal.ZERO;

    /** 近 7 日趋势 */
    private List<TrendPoint> trend = new ArrayList<>();

    /** 热销商品 Top 5 */
    private List<TopProduct> topProducts = new ArrayList<>();

    @Data
    public static class TrendPoint implements Serializable {
        private static final long serialVersionUID = 1L;
        private String date;
        private long orders;
        private BigDecimal sales = BigDecimal.ZERO;
    }

    @Data
    public static class TopProduct implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long productId;
        private String name;
        private String mainImage;
        private long quantity;
        private BigDecimal amount = BigDecimal.ZERO;
    }
}
