package com.lifeshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifeshop.common.BusinessException;
import com.lifeshop.common.PageResult;
import com.lifeshop.dto.OrderItemDTO;
import com.lifeshop.dto.OrderSubmitDTO;
import com.lifeshop.entity.Address;
import com.lifeshop.entity.CartItem;
import com.lifeshop.entity.Merchant;
import com.lifeshop.entity.Order;
import com.lifeshop.entity.OrderItem;
import com.lifeshop.entity.Product;
import com.lifeshop.entity.User;
import com.lifeshop.mapper.AddressMapper;
import com.lifeshop.mapper.CartItemMapper;
import com.lifeshop.mapper.MerchantMapper;
import com.lifeshop.mapper.OrderItemMapper;
import com.lifeshop.mapper.OrderMapper;
import com.lifeshop.mapper.ProductMapper;
import com.lifeshop.mapper.UserMapper;
import com.lifeshop.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 订单服务
 */
@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final CartItemMapper cartItemMapper;
    private final AddressMapper addressMapper;
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final LogisticsService logisticsService;
    private final CouponService couponService;

    @Value("${lifeshop.order.freight:10}")
    private BigDecimal freight;

    @Value("${lifeshop.order.free-freight-threshold:99}")
    private BigDecimal freeFreightThreshold;

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public OrderService(OrderMapper orderMapper, OrderItemMapper orderItemMapper, ProductMapper productMapper,
                        CartItemMapper cartItemMapper, AddressMapper addressMapper, UserMapper userMapper,
                        MerchantMapper merchantMapper, LogisticsService logisticsService, CouponService couponService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.cartItemMapper = cartItemMapper;
        this.addressMapper = addressMapper;
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.logisticsService = logisticsService;
        this.couponService = couponService;
    }

    /** 提交订单（按店铺拆单，返回本次生成的订单列表） */
    @Transactional(rollbackFor = Exception.class)
    public List<OrderVO> submit(Long userId, OrderSubmitDTO dto) {
        Address address = addressMapper.selectById(dto.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("收货地址不存在");
        }

        boolean fromCart = dto.getItems() == null || dto.getItems().isEmpty();
        List<CartItem> sources = new ArrayList<>();
        if (fromCart) {
            sources = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, userId)
                    .eq(CartItem::getChecked, 1));
            if (sources.isEmpty()) {
                throw new BusinessException("请先勾选要结算的商品");
            }
        } else {
            for (OrderItemDTO item : dto.getItems()) {
                if (item.getProductId() == null) {
                    continue;
                }
                CartItem temp = new CartItem();
                temp.setProductId(item.getProductId());
                temp.setQuantity(item.getQuantity() == null || item.getQuantity() < 1 ? 1 : item.getQuantity());
                sources.add(temp);
            }
            if (sources.isEmpty()) {
                throw new BusinessException("请选择要购买的商品");
            }
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem source : sources) {
            Product product = productMapper.selectById(source.getProductId());
            if (product == null) {
                throw new BusinessException("商品已不存在，请刷新后重试");
            }
            if (product.getStatus() == null || product.getStatus() != 1) {
                throw new BusinessException("「" + product.getName() + "」已下架");
            }
            int quantity = source.getQuantity() == null || source.getQuantity() < 1 ? 1 : source.getQuantity();
            if (product.getStock() == null || product.getStock() < quantity) {
                throw new BusinessException("「" + product.getName() + "」库存不足");
            }
            // 条件更新扣减库存，防止并发超卖
            int rows = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .setSql("stock = stock - " + quantity + ", sales = sales + " + quantity)
                    .eq(Product::getId, product.getId())
                    .ge(Product::getStock, quantity));
            if (rows <= 0) {
                throw new BusinessException("「" + product.getName() + "」库存不足");
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            // 记下所属店铺，商家端按这个字段查自己的订单
            orderItem.setMerchantId(product.getMerchantId() == null ? 0L : product.getMerchantId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(quantity);
            orderItem.setTotalPrice(subtotal);
            orderItems.add(orderItem);
        }

        // 按店铺拆单：一次结算里混了多个店铺的商品时，每个店铺生成一笔独立订单
        Map<Long, List<OrderItem>> groups = new LinkedHashMap<>();
        for (OrderItem item : orderItems) {
            groups.computeIfAbsent(item.getMerchantId(), key -> new ArrayList<>()).add(item);
        }
        Map<Long, String> merchantNames = loadMerchantNames(groups.keySet());

        // 优惠券：整单抵扣，按各子订单金额占比分摊
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (dto.getUserCouponId() != null) {
            couponDiscount = couponService.use(userId, dto.getUserCouponId(), totalAmount);
        }

        List<Long> createdIds = new ArrayList<>();
        for (Map.Entry<Long, List<OrderItem>> group : groups.entrySet()) {
            List<OrderItem> items = group.getValue();
            BigDecimal groupAmount = items.stream()
                    .map(OrderItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal groupFreight = groupAmount.compareTo(freeFreightThreshold) >= 0 ? BigDecimal.ZERO : freight;
            String merchantName = merchantNames.getOrDefault(group.getKey(), "平台自营");

            // 按金额占比分摊优惠券抵扣（保留 2 位小数，最后一单兜底）
            BigDecimal groupDiscount = BigDecimal.ZERO;
            if (couponDiscount.compareTo(BigDecimal.ZERO) > 0 && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                groupDiscount = couponDiscount.multiply(groupAmount)
                        .divide(totalAmount, 2, java.math.RoundingMode.HALF_UP);
            }

            Order order = new Order();
            order.setOrderNo(generateOrderNo());
            order.setUserId(userId);
            order.setMerchantId(group.getKey());
            order.setMerchantName(merchantName);
            order.setTotalAmount(groupAmount);
            order.setFreight(groupFreight);
            order.setDiscount(groupDiscount);
            order.setPayAmount(groupAmount.add(groupFreight).subtract(groupDiscount).max(BigDecimal.ZERO));
            order.setStatus(Order.STATUS_UNPAID);
            order.setReceiver(address.getReceiver());
            order.setPhone(address.getPhone());
            order.setAddress(fullAddress(address));
            order.setRemark(dto.getRemark());
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.insert(order);

            // 记录优惠券使用的订单号（绑定到第一个订单，用于取消时退回）
            if (couponDiscount.compareTo(BigDecimal.ZERO) > 0 && createdIds.isEmpty()) {
                couponService.bindOrderNo(userId, dto.getUserCouponId(), order.getOrderNo());
            }

            for (OrderItem orderItem : items) {
                orderItem.setOrderId(order.getId());
                orderItem.setOrderNo(order.getOrderNo());
                orderItemMapper.insert(orderItem);
            }
            createdIds.add(order.getId());
        }

        if (fromCart) {
            cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, userId)
                    .eq(CartItem::getChecked, 1));
        }

        List<OrderVO> result = new ArrayList<>();
        for (Long id : createdIds) {
            result.add(detail(userId, id));
        }
        return result;
    }

    /** 店铺名称（拆单时写入订单快照） */
    private Map<Long, String> loadMerchantNames(java.util.Set<Long> merchantIds) {
        Map<Long, String> names = new HashMap<>();
        List<Long> valid = merchantIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
        if (valid.isEmpty()) {
            return names;
        }
        for (Merchant merchant : merchantMapper.selectBatchIds(valid)) {
            names.put(merchant.getId(), merchant.getName());
        }
        return names;
    }

    /** 模拟支付 */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO pay(Long userId, Long orderId) {
        Order order = getOwnOrder(userId, orderId);
        if (order.getStatus() != Order.STATUS_UNPAID) {
            throw new BusinessException("当前订单状态不可支付");
        }
        order.setStatus(Order.STATUS_UNSHIPPED);
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return detail(userId, orderId);
    }

    /** 取消订单（仅待付款），并归还库存 */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long orderId) {
        Order order = getOwnOrder(userId, orderId);
        if (order.getStatus() != Order.STATUS_UNPAID) {
            throw new BusinessException("当前订单状态不可取消");
        }
        order.setStatus(Order.STATUS_CANCELED);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        restoreStock(orderId);
        // 退回本单使用的优惠券
        couponService.refundByOrderNo(order.getOrderNo());
    }

    /** 确认收货 */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO confirm(Long userId, Long orderId) {
        Order order = getOwnOrder(userId, orderId);
        if (order.getStatus() != Order.STATUS_SHIPPED) {
            throw new BusinessException("当前订单状态不可确认收货");
        }
        order.setStatus(Order.STATUS_FINISHED);
        order.setFinishTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        // 补齐完整运输轨迹
        logisticsService.completeOnFinish(order);
        // 货款入账到商家余额
        settleToMerchant(order);
        return detail(userId, orderId);
    }

    /** 订单完成后，货款（实付金额）入账到商家可提现余额 */
    private void settleToMerchant(Order order) {
        if (order.getMerchantId() == null || order.getMerchantId() == 0) {
            return;
        }
        BigDecimal amount = order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        merchantMapper.update(null, new LambdaUpdateWrapper<Merchant>()
                .setSql("balance = balance + " + amount)
                .eq(Merchant::getId, order.getMerchantId()));
    }

    /** 用户订单分页 */
    public PageResult<OrderVO> page(Long userId, Integer status, long pageNum, long pageSize) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getId);
        Page<Order> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return toPageResult(page, false);
    }

    /** 后台订单分页 */
    public PageResult<OrderVO> adminPage(String keyword, Integer status, long pageNum, long pageSize) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(Order::getOrderNo, keyword)
                    .or().like(Order::getReceiver, keyword)
                    .or().like(Order::getPhone, keyword));
        }
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getId);
        Page<Order> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return toPageResult(page, true);
    }

    /** 订单详情（用户维度校验归属） */
    public OrderVO detail(Long userId, Long orderId) {
        return toVO(getOwnOrder(userId, orderId), listItems(orderId), false);
    }

    /** 后台订单详情 */
    public OrderVO adminDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        OrderVO vo = toVO(order, listItems(orderId), true);
        return vo;
    }

    // ---------------------------------------------------------------- 商家端

    /** 商家：只查自己店铺的订单 */
    public PageResult<OrderVO> merchantPage(Long merchantId, String keyword, Integer status,
                                            long pageNum, long pageSize) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(Order::getOrderNo, keyword)
                    .or().like(Order::getReceiver, keyword)
                    .or().like(Order::getPhone, keyword));
        }
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getId);
        Page<Order> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return toPageResult(page, true);
    }

    /** 商家：订单详情（校验店铺归属） */
    public OrderVO merchantDetail(Long merchantId, Long orderId) {
        Order order = getOwnMerchantOrder(merchantId, orderId);
        List<OrderItem> items = listItems(orderId).stream()
                .filter(item -> item.getMerchantId() != null && item.getMerchantId().equals(merchantId))
                .collect(Collectors.toList());
        return toVO(order, items, true);
    }

    /** 商家：发货（只有本店的待发货订单可以操作，需填写物流单号） */
    public void merchantShip(Long merchantId, Long orderId, String trackingNo, String company) {
        Order order = getOwnMerchantOrder(merchantId, orderId);
        if (order.getStatus() != Order.STATUS_UNSHIPPED) {
            throw new BusinessException("仅待发货订单可以发货");
        }
        if (!StringUtils.hasText(trackingNo)) {
            throw new BusinessException("请填写物流单号");
        }
        order.setStatus(Order.STATUS_SHIPPED);
        order.setShipTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        // 写入物流单号与揽收节点
        logisticsService.createOnShip(order, trackingNo, company);
    }

    /** 商家：待发货数量（后台角标） */
    public Long merchantPendingShip(Long merchantId) {
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId)
                .eq(Order::getStatus, Order.STATUS_UNSHIPPED));
        return count == null ? 0L : count;
    }

    private Order getOwnMerchantOrder(Long merchantId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getMerchantId() == null || !order.getMerchantId().equals(merchantId)) {
            throw new BusinessException("订单不存在或不属于当前店铺");
        }
        return order;
    }

    /** 后台发货 */
    public void ship(Long orderId, String trackingNo, String company) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != Order.STATUS_UNSHIPPED) {
            throw new BusinessException("仅待发货订单可以发货");
        }
        if (!StringUtils.hasText(trackingNo)) {
            throw new BusinessException("请填写物流单号");
        }
        order.setStatus(Order.STATUS_SHIPPED);
        order.setShipTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        // 写入物流单号与揽收节点
        logisticsService.createOnShip(order, trackingNo, company);
    }

    /** 各状态订单数量（用户中心角标） */
    public Map<String, Long> countByStatus(Long userId) {
        Map<String, Long> result = new HashMap<>();
        for (int status = 0; status <= 4; status++) {
            Long count = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                    .eq(Order::getUserId, userId)
                    .eq(Order::getStatus, status));
            result.put(String.valueOf(status), count == null ? 0L : count);
        }
        return result;
    }

    /** 待付款订单数量（后台角标） */
    public Long countPendingShip() {
        Long count = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, Order.STATUS_UNSHIPPED));
        return count == null ? 0L : count;
    }

    // ------------------------------------------------------------------

    private Order getOwnOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private List<OrderItem> listItems(Long orderId) {
        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }

    private void restoreStock(Long orderId) {
        List<OrderItem> items = listItems(orderId);
        for (OrderItem item : items) {
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .setSql("stock = stock + " + item.getQuantity()
                            + ", sales = GREATEST(sales - " + item.getQuantity() + ", 0)")
                    .eq(Product::getId, item.getProductId()));
        }
    }

    private PageResult<OrderVO> toPageResult(Page<Order> page, boolean withUsername) {
        List<Order> records = page.getRecords();
        Map<Long, List<OrderItem>> itemMap = new HashMap<>();
        Map<Long, String> usernameMap = new HashMap<>();

        if (!records.isEmpty()) {
            List<Long> orderIds = records.stream().map(Order::getId).collect(Collectors.toList());
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
            itemMap = items.stream().collect(Collectors.groupingBy(OrderItem::getOrderId));

            if (withUsername) {
                List<Long> userIds = records.stream().map(Order::getUserId).distinct().collect(Collectors.toList());
                List<User> users = userMapper.selectBatchIds(userIds);
                usernameMap = users.stream().collect(Collectors.toMap(
                        User::getId, u -> u.getNickname() == null ? u.getUsername() : u.getNickname(), (a, b) -> a));
            }
        }

        List<OrderVO> vos = new ArrayList<>();
        for (Order order : records) {
            List<OrderItem> items = itemMap.get(order.getId());
            OrderVO vo = toVO(order, items == null ? new ArrayList<>() : items, withUsername);
            if (withUsername) {
                vo.setUsername(usernameMap.get(order.getUserId()));
            }
            vos.add(vo);
        }
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), vos);
    }

    private OrderVO toVO(Order order, List<OrderItem> items, boolean withUsername) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setItems(items == null ? new ArrayList<>() : items);
        if (withUsername) {
            User user = userMapper.selectById(order.getUserId());
            if (user != null) {
                vo.setUsername(user.getNickname() == null ? user.getUsername() : user.getNickname());
            }
        }
        return vo;
    }

    private String fullAddress(Address address) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(address.getProvince())) {
            sb.append(address.getProvince());
        }
        if (StringUtils.hasText(address.getCity())) {
            sb.append(address.getCity());
        }
        if (StringUtils.hasText(address.getDistrict())) {
            sb.append(address.getDistrict());
        }
        sb.append(address.getDetail() == null ? "" : address.getDetail());
        return sb.toString();
    }

    private String generateOrderNo() {
        return "LS" + LocalDateTime.now().format(ORDER_NO_FORMAT)
                + String.format("%04d", new Random().nextInt(10000));
    }
}
