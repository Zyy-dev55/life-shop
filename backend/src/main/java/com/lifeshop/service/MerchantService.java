package com.lifeshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifeshop.common.BusinessException;
import com.lifeshop.common.PageResult;
import com.lifeshop.common.UserContext;
import com.lifeshop.dto.AdminMerchantCreateDTO;
import com.lifeshop.entity.Consult;
import com.lifeshop.entity.Merchant;
import com.lifeshop.entity.Order;
import com.lifeshop.entity.OrderItem;
import com.lifeshop.entity.Product;
import com.lifeshop.entity.Review;
import com.lifeshop.entity.User;
import com.lifeshop.mapper.ConsultMapper;
import com.lifeshop.mapper.MerchantMapper;
import com.lifeshop.mapper.OrderItemMapper;
import com.lifeshop.mapper.OrderMapper;
import com.lifeshop.mapper.ProductMapper;
import com.lifeshop.mapper.ReviewMapper;
import com.lifeshop.mapper.UserMapper;
import com.lifeshop.utils.PasswordUtil;
import com.lifeshop.vo.MerchantDashboardVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商家后台：店铺身份、店铺资料、经营概览
 */
@Service
public class MerchantService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MM-dd");

    private final MerchantMapper merchantMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ReviewMapper reviewMapper;
    private final ConsultMapper consultMapper;
    private final UserMapper userMapper;

    public MerchantService(MerchantMapper merchantMapper, ProductMapper productMapper, OrderMapper orderMapper,
                           OrderItemMapper orderItemMapper, ReviewMapper reviewMapper, ConsultMapper consultMapper,
                           UserMapper userMapper) {
        this.merchantMapper = merchantMapper;
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.reviewMapper = reviewMapper;
        this.consultMapper = consultMapper;
        this.userMapper = userMapper;
    }

    /** 当前登录商家（未开通店铺 / 店铺停业时拒绝） */
    public Merchant current() {
        Long userId = UserContext.getUserId();
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getUserId, userId));
        if (merchant == null) {
            throw new BusinessException(403, "当前账号还未开通店铺");
        }
        if (merchant.getStatus() == null || merchant.getStatus() != 1) {
            throw new BusinessException(403, "店铺已停止营业，请联系平台运营");
        }
        return merchant;
    }

    /** 当前商家 ID，商品/订单/评价的查询都以它作为数据边界 */
    public Long currentId() {
        return current().getId();
    }

    /** 店铺资料（带在售商品数） */
    public Merchant profile() {
        Merchant merchant = current();
        Long count = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getMerchantId, merchant.getId())
                .eq(Product::getStatus, 1));
        merchant.setProductCount(count == null ? 0L : count);
        merchant.setUsername(UserContext.get().getUsername());
        return merchant;
    }

    /** 修改店铺资料（名称 / 简介 / 联系方式 / 地址 / logo） */
    public Merchant updateProfile(Merchant form) {
        Merchant merchant = current();
        if (!StringUtils.hasText(form.getName())) {
            throw new BusinessException("店铺名称不能为空");
        }
        merchant.setName(form.getName().trim());
        merchant.setLogo(form.getLogo());
        merchant.setDescription(form.getDescription());
        merchant.setContact(form.getContact());
        merchant.setPhone(form.getPhone());
        merchant.setAddress(form.getAddress());
        merchant.setUpdateTime(LocalDateTime.now());
        merchantMapper.updateById(merchant);
        return profile();
    }

    /** 经营概览 */
    public MerchantDashboardVO dashboard() {
        Merchant merchant = current();
        Long mid = merchant.getId();

        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, mid));

        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().eq(Order::getMerchantId, mid));

        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getMerchantId, mid));

        MerchantDashboardVO vo = new MerchantDashboardVO();
        vo.setMerchantId(mid);
        vo.setShopName(merchant.getName());
        vo.setContact(merchant.getContact());

        vo.setProductCount(products.size());
        vo.setOnSaleCount(products.stream().filter(p -> p.getStatus() != null && p.getStatus() == 1).count());
        vo.setLowStockCount(products.stream()
                .filter(p -> p.getStock() != null && p.getStock() < 10)
                .count());

        // 已支付订单才计入销售额（状态 1 待发货 / 2 已发货 / 3 已完成）
        List<Order> paidOrders = orders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() >= Order.STATUS_UNSHIPPED
                        && o.getStatus() <= Order.STATUS_FINISHED)
                .collect(Collectors.toList());

        vo.setOrderCount(orders.size());
        vo.setPendingShipCount(orders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() == Order.STATUS_UNSHIPPED)
                .count());

        LocalDate today = LocalDate.now();
        List<Order> todayOrders = orders.stream()
                .filter(o -> o.getCreateTime() != null && today.equals(o.getCreateTime().toLocalDate()))
                .collect(Collectors.toList());
        vo.setTodayOrderCount(todayOrders.size());
        vo.setTodaySales(sum(todayOrders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() >= Order.STATUS_UNSHIPPED
                        && o.getStatus() <= Order.STATUS_FINISHED)
                .collect(Collectors.toList())));
        vo.setTotalSales(sum(paidOrders));

        vo.setUnrepliedCount(reviews.stream()
                .filter(r -> !StringUtils.hasText(r.getReply()))
                .count());

        // 待回复的客户咨询：自己店铺下未读消息总数
        List<Consult> consults = consultMapper.selectList(new LambdaQueryWrapper<Consult>()
                .eq(Consult::getMerchantId, mid).gt(Consult::getMerchantUnread, 0));
        vo.setConsultUnread(consults.stream()
                .mapToLong(c -> c.getMerchantUnread() == null ? 0 : c.getMerchantUnread())
                .sum());

        if (!reviews.isEmpty()) {
            double avg = reviews.stream()
                    .filter(r -> r.getRating() != null)
                    .mapToInt(Review::getRating)
                    .average().orElse(0);
            vo.setAvgRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        }

        // 近 7 日趋势
        Map<String, MerchantDashboardVO.TrendPoint> points = new HashMap<>();
        List<MerchantDashboardVO.TrendPoint> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String label = LocalDate.now().minusDays(i).format(DAY);
            MerchantDashboardVO.TrendPoint point = new MerchantDashboardVO.TrendPoint();
            point.setDate(label);
            points.put(label, point);
            trend.add(point);
        }
        for (Order order : orders) {
            if (order.getCreateTime() == null) {
                continue;
            }
            MerchantDashboardVO.TrendPoint point = points.get(order.getCreateTime().toLocalDate().format(DAY));
            if (point == null) {
                continue;
            }
            point.setOrders(point.getOrders() + 1);
            if (order.getStatus() != null && order.getStatus() >= Order.STATUS_UNSHIPPED
                    && order.getStatus() <= Order.STATUS_FINISHED) {
                point.setSales(point.getSales().add(nullToZero(order.getPayAmount())));
            }
        }
        vo.setTrend(trend);

        // 热销商品 Top 5（按已支付订单的明细数量聚合）
        vo.setTopProducts(topProducts(mid, paidOrders));

        return vo;
    }

    private List<MerchantDashboardVO.TopProduct> topProducts(Long merchantId, List<Order> paidOrders) {
        if (paidOrders.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> orderIds = paidOrders.stream().map(Order::getId).collect(Collectors.toList());
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));

        Map<Long, MerchantDashboardVO.TopProduct> map = new HashMap<>();
        for (OrderItem item : items) {
            if (item.getMerchantId() == null || !item.getMerchantId().equals(merchantId)) {
                continue;
            }
            MerchantDashboardVO.TopProduct top = map.get(item.getProductId());
            if (top == null) {
                top = new MerchantDashboardVO.TopProduct();
                top.setProductId(item.getProductId());
                top.setName(item.getProductName());
                top.setMainImage(item.getProductImage());
                map.put(item.getProductId(), top);
            }
            long quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            top.setQuantity(top.getQuantity() + quantity);
            top.setAmount(top.getAmount().add(nullToZero(item.getTotalPrice())));
        }
        return map.values().stream()
                .sorted(Comparator.comparingLong(MerchantDashboardVO.TopProduct::getQuantity).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    private BigDecimal sum(List<Order> orders) {
        return orders.stream()
                .map(o -> nullToZero(o.getPayAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // ------------------------------------------------------------ 平台端（商家管理）

    /** 平台管理员：商家（店铺）分页，附带登录账号与在售商品数 */
    public PageResult<Merchant> adminPage(String keyword, Integer status, long pageNum, long pageSize) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(Merchant::getName, keyword)
                    .or().like(Merchant::getContact, keyword)
                    .or().like(Merchant::getPhone, keyword));
        }
        if (status != null) {
            wrapper.eq(Merchant::getStatus, status);
        }
        wrapper.orderByDesc(Merchant::getId);
        Page<Merchant> page = merchantMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 填充登录账号与在售商品数
        for (Merchant merchant : page.getRecords()) {
            User user = userMapper.selectById(merchant.getUserId());
            if (user != null) {
                merchant.setUsername(user.getUsername());
            }
            Long productCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                    .eq(Product::getMerchantId, merchant.getId())
                    .eq(Product::getStatus, 1));
            merchant.setProductCount(productCount == null ? 0L : productCount);
        }
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    /** 平台管理员：新增商家（创建商家账号 + 店铺） */
    @Transactional(rollbackFor = Exception.class)
    public Merchant adminCreate(AdminMerchantCreateDTO dto) {
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该账号已被占用");
        }

        // 1. 创建商家账号
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(PasswordUtil.encrypt(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getContact()) ? dto.getContact() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setGender(0);
        user.setRole("MERCHANT");
        user.setStatus(1);
        user.setAvatar("");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        // 2. 创建店铺
        Merchant merchant = new Merchant();
        merchant.setUserId(user.getId());
        merchant.setName(dto.getShopName().trim());
        merchant.setLogo(dto.getLogo());
        merchant.setDescription(dto.getDescription());
        merchant.setContact(dto.getContact());
        merchant.setPhone(dto.getPhone());
        merchant.setAddress(dto.getAddress());
        merchant.setStatus(1);
        merchant.setCreateTime(LocalDateTime.now());
        merchant.setUpdateTime(LocalDateTime.now());
        merchantMapper.insert(merchant);

        merchant.setUsername(user.getUsername());
        return merchant;
    }

    /** 平台管理员：启用 / 停业店铺（同步商家账号状态） */
    @Transactional(rollbackFor = Exception.class)
    public void adminUpdateStatus(Long merchantId, Integer status) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("店铺不存在");
        }
        merchant.setStatus(status);
        merchant.setUpdateTime(LocalDateTime.now());
        merchantMapper.updateById(merchant);

        User user = userMapper.selectById(merchant.getUserId());
        if (user != null) {
            user.setStatus(status == null || status != 1 ? 0 : 1);
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }
}
