package com.lifeshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifeshop.entity.Order;
import com.lifeshop.entity.Product;
import com.lifeshop.entity.User;
import com.lifeshop.mapper.OrderMapper;
import com.lifeshop.mapper.ProductMapper;
import com.lifeshop.mapper.UserMapper;
import com.lifeshop.vo.DashboardVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台数据看板
 */
@Service
public class StatisticsService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("MM-dd");
    private static final String[] STATUS_NAMES = {"待付款", "待发货", "待收货", "已完成", "已取消"};

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;

    public StatisticsService(OrderMapper orderMapper, UserMapper userMapper, ProductMapper productMapper) {
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
    }

    /** 已支付订单状态 */
    private static final Integer[] PAID_STATUS = {1, 2, 3};

    public DashboardVO dashboard() {
        DashboardVO vo = new DashboardVO();

        // 累计销售额 / 今日销售额
        vo.setTotalSales(sumAmount(null));
        vo.setTodaySales(sumAmount(LocalDate.now().atStartOfDay()));

        // 订单数
        Long orderCount = orderMapper.selectCount(null);
        vo.setOrderCount(orderCount == null ? 0L : orderCount);
        Long todayOrderCount = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, LocalDate.now().atStartOfDay()));
        vo.setTodayOrderCount(todayOrderCount == null ? 0L : todayOrderCount);

        // 用户数 / 商品数
        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "USER"));
        vo.setUserCount(userCount == null ? 0L : userCount);
        Long productCount = productMapper.selectCount(null);
        vo.setProductCount(productCount == null ? 0L : productCount);

        // 待发货
        Long pendingShip = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, Order.STATUS_UNSHIPPED));
        vo.setPendingShipCount(pendingShip == null ? 0L : pendingShip);

        // 近 7 天销售趋势
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();

            List<Map<String, Object>> rows = orderMapper.selectMaps(new QueryWrapper<Order>()
                    .select("IFNULL(SUM(pay_amount),0) AS amount", "COUNT(*) AS cnt")
                    .in("status", PAID_STATUS)
                    .ge("create_time", start)
                    .lt("create_time", end));

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", day.format(DAY_FORMAT));
            point.put("amount", rows.isEmpty() ? BigDecimal.ZERO : toDecimal(rows.get(0).get("amount")));
            point.put("count", rows.isEmpty() ? 0L : toLong(rows.get(0).get("cnt")));
            trend.add(point);
        }
        vo.setSalesTrend(trend);

        // 订单状态分布
        List<Map<String, Object>> distribution = new ArrayList<>();
        for (int status = 0; status <= 4; status++) {
            Long count = orderMapper.selectCount(
                    new LambdaQueryWrapper<Order>().eq(Order::getStatus, status));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("status", status);
            item.put("name", STATUS_NAMES[status]);
            item.put("count", count == null ? 0L : count);
            distribution.add(item);
        }
        vo.setStatusDistribution(distribution);

        // 热销 TOP5
        Page<Product> hotPage = productMapper.selectPage(new Page<>(1, 5),
                new LambdaQueryWrapper<Product>().orderByDesc(Product::getSales));
        vo.setHotProducts(hotPage.getRecords());

        return vo;
    }

    /** 统计已支付订单金额，startTime 为空表示全部 */
    private BigDecimal sumAmount(LocalDateTime startTime) {
        QueryWrapper<Order> wrapper = new QueryWrapper<Order>()
                .select("IFNULL(SUM(pay_amount),0) AS total")
                .in("status", PAID_STATUS);
        if (startTime != null) {
            wrapper.ge("create_time", startTime);
        }
        List<Map<String, Object>> rows = orderMapper.selectMaps(wrapper);
        if (rows.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return toDecimal(rows.get(0).get("total"));
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(value.toString());
    }
}
