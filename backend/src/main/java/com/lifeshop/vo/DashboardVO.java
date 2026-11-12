package com.lifeshop.vo;

import com.lifeshop.entity.Product;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 后台数据看板
 */
@Data
public class DashboardVO implements Serializable {

    /** 累计销售额（已付款订单） */
    private BigDecimal totalSales = BigDecimal.ZERO;

    /** 今日销售额 */
    private BigDecimal todaySales = BigDecimal.ZERO;

    /** 累计订单数 */
    private Long orderCount = 0L;

    /** 今日订单数 */
    private Long todayOrderCount = 0L;

    /** 用户数 */
    private Long userCount = 0L;

    /** 商品数 */
    private Long productCount = 0L;

    /** 待发货订单数 */
    private Long pendingShipCount = 0L;

    /** 近 7 天销售趋势 [{date, amount, count}] */
    private List<Map<String, Object>> salesTrend = new ArrayList<>();

    /** 订单状态分布 [{status, count}] */
    private List<Map<String, Object>> statusDistribution = new ArrayList<>();

    /** 热销商品 TOP5 */
    private List<Product> hotProducts = new ArrayList<>();
}
