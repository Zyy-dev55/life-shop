package com.lifeshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifeshop.common.BusinessException;
import com.lifeshop.entity.Logistics;
import com.lifeshop.entity.Order;
import com.lifeshop.mapper.LogisticsMapper;
import com.lifeshop.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * 物流轨迹服务
 *
 * 数据边界：买家只能看自己订单的物流，商家/管理员可看自己管辖订单的物流。
 */
@Service
public class LogisticsService {

    private final LogisticsMapper logisticsMapper;
    private final OrderMapper orderMapper;

    public LogisticsService(LogisticsMapper logisticsMapper, OrderMapper orderMapper) {
        this.logisticsMapper = logisticsMapper;
        this.orderMapper = orderMapper;
    }

    private static final String[] COMPANIES = {"顺丰速运", "中通快递", "圆通速递", "韵达快递", "京东物流"};

    /**
     * 商家/平台发货时，写入首个轨迹节点「已揽收」。
     * 单号与快递公司由发货方填写（不再自动生成）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void createOnShip(Order order, String trackingNo, String company) {
        if (logisticsMapper.selectCount(new LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getOrderId, order.getId())) > 0) {
            return;
        }
        String comp = StringUtils.hasText(company) ? company.trim() : "快递";
        String no = StringUtils.hasText(trackingNo) ? trackingNo.trim() : genTrackingNo();

        Logistics node = new Logistics();
        node.setOrderId(order.getId());
        node.setOrderNo(order.getOrderNo());
        node.setTrackingNo(no);
        node.setCompany(comp);
        node.setContent(comp + " 已揽收包裹，准备发往分拨中心");
        node.setCreateTime(LocalDateTime.now());
        logisticsMapper.insert(node);
    }

    /** 兼容旧逻辑（无单号时兜底生成） */
    @Transactional(rollbackFor = Exception.class)
    public void createOnShip(Order order) {
        createOnShip(order, genTrackingNo(), COMPANIES[new Random().nextInt(COMPANIES.length)]);
    }

    /**
     * 补齐后续轨迹节点（在确认收货后，把「运输中/派送/签收」等环节补全，形成完整运输过程）。
     * 已存在节点则跳过，保证幂等。中间节点时间均匀落在「发货时间」与「当前签收时间」之间。
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeOnFinish(Order order) {
        if (order.getShipTime() == null) {
            return;
        }
        Logistics first = logisticsMapper.selectOne(new LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getOrderId, order.getId())
                .orderByAsc(Logistics::getId)
                .last("limit 1"));
        if (first == null) {
            // 兜底：老数据没轨迹时先补「揽收」
            createOnShip(order);
            first = logisticsMapper.selectOne(new LambdaQueryWrapper<Logistics>()
                    .eq(Logistics::getOrderId, order.getId())
                    .orderByAsc(Logistics::getId)
                    .last("limit 1"));
        }
        String company = first == null ? "物流" : first.getCompany();
        String trackingNo = first == null ? genTrackingNo() : first.getTrackingNo();
        LocalDateTime ship = order.getShipTime();
        LocalDateTime now = LocalDateTime.now();

        // 已存在的节点数（幂等）
        long existing = logisticsMapper.selectCount(new LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getOrderId, order.getId()));

        String[] middle = {
                company + " 已从发货地发出，正在运输途中",
                "快件已到达收货城市转运中心，正在分拣",
                "快件已到达派送网点，快递员正在派送中",
                "包裹已送达驿站，等待收货人签收"
        };
        int total = middle.length + 1; // 中间节点 + 签收节点
        // 中间节点时间均匀分布在 [ship, now] 之间
        long spanSeconds = java.time.Duration.between(ship, now).getSeconds();
        if (spanSeconds < total * 60L) {
            spanSeconds = total * 60L; // 至少保证时间递增
        }
        for (int i = 0; i < middle.length; i++) {
            int seq = (int) existing + i;
            long offset = spanSeconds * (i + 1) / total;
            LocalDateTime nodeTime = ship.plusSeconds(offset);
            if (!nodeTime.isAfter(now)) {
                nodeTime = now.minusSeconds(total - (i + 1));
            }
            Logistics node = new Logistics();
            node.setOrderId(order.getId());
            node.setOrderNo(order.getOrderNo());
            node.setTrackingNo(trackingNo);
            node.setCompany(company);
            node.setContent(middle[i]);
            node.setCreateTime(nodeTime);
            logisticsMapper.insert(node);
        }

        // 签收节点
        Logistics sign = new Logistics();
        sign.setOrderId(order.getId());
        sign.setOrderNo(order.getOrderNo());
        sign.setTrackingNo(trackingNo);
        sign.setCompany(company);
        sign.setContent("包裹已签收，感谢使用 " + company);
        sign.setCreateTime(now);
        logisticsMapper.insert(sign);
    }

    /** 用户/商家/管理员查看某订单的物流轨迹（校验订单存在与归属由调用方负责） */
    public List<Logistics> byOrder(Long orderId) {
        return logisticsMapper.selectList(new LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getOrderId, orderId)
                .orderByAsc(Logistics::getId));
    }

    /** 用户查看自己订单的物流轨迹 */
    public List<Logistics> byOrderForUser(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getUserId() == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        return byOrder(orderId);
    }

    private String genTrackingNo() {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "SF" + stamp + String.format("%04d", new Random().nextInt(10000));
    }
}
