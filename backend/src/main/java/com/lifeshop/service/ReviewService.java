package com.lifeshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifeshop.common.BusinessException;
import com.lifeshop.common.PageResult;
import com.lifeshop.dto.ReviewSubmitDTO;
import com.lifeshop.entity.Order;
import com.lifeshop.entity.OrderItem;
import com.lifeshop.entity.Product;
import com.lifeshop.entity.Review;
import com.lifeshop.entity.User;
import com.lifeshop.mapper.OrderItemMapper;
import com.lifeshop.mapper.OrderMapper;
import com.lifeshop.mapper.ProductMapper;
import com.lifeshop.mapper.ReviewMapper;
import com.lifeshop.mapper.UserMapper;
import com.lifeshop.vo.PendingReviewVO;
import com.lifeshop.vo.ReviewStatsVO;
import com.lifeshop.vo.ReviewVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评价服务
 */
@Service
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;

    public ReviewService(ReviewMapper reviewMapper, OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                         UserMapper userMapper, ProductMapper productMapper) {
        this.reviewMapper = reviewMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
    }

    /** 商品评价统计 */
    public ReviewStatsVO stats(Long productId) {
        List<Review> list = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getProductId, productId));
        ReviewStatsVO vo = new ReviewStatsVO();
        vo.setTotal(list.size());
        Map<Integer, Long> starCount = new HashMap<>();
        int sum = 0;
        int good = 0;
        int withImage = 0;
        for (int i = 1; i <= 5; i++) {
            starCount.put(i, 0L);
        }
        for (Review review : list) {
            int rating = review.getRating() == null ? 5 : review.getRating();
            starCount.put(rating, starCount.get(rating) + 1);
            sum += rating;
            if (rating >= 4) {
                good++;
            }
            if (StringUtils.hasText(review.getImages())) {
                withImage++;
            }
        }
        vo.setStarCount(starCount);
        vo.setWithImage(withImage);
        if (!list.isEmpty()) {
            vo.setAvgRating(BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(list.size()), 1, RoundingMode.HALF_UP));
            vo.setGoodRate((int) Math.round(good * 100.0 / list.size()));
        }
        return vo;
    }

    /** 商品评价分页 */
    public PageResult<ReviewVO> pageByProduct(Long productId, Integer rating, Integer withImage,
                                              long pageNum, long pageSize) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>().eq(Review::getProductId, productId);
        if (rating != null && rating > 0) {
            wrapper.eq(Review::getRating, rating);
        }
        if (withImage != null && withImage == 1) {
            wrapper.isNotNull(Review::getImages).ne(Review::getImages, "");
        }
        wrapper.orderByDesc(Review::getId);
        Page<Review> page = reviewMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(),
                page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    /** 我的评价 */
    public PageResult<ReviewVO> pageByUser(Long userId, long pageNum, long pageSize) {
        Page<Review> page = reviewMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Review>().eq(Review::getUserId, userId).orderByDesc(Review::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(),
                page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    /** 待评价列表：已完成订单里还没评价的商品 */
    public List<PendingReviewVO> pending(Long userId) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getStatus, Order.STATUS_FINISHED)
                .orderByDesc(Order::getId));
        if (orders.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
        List<Review> reviewed = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getUserId, userId));
        Set<String> done = new HashSet<>();
        for (Review review : reviewed) {
            done.add(review.getOrderId() + "-" + review.getProductId());
        }

        Map<Long, Order> orderMap = orders.stream().collect(Collectors.toMap(Order::getId, o -> o, (a, b) -> a));
        List<PendingReviewVO> result = new ArrayList<>();
        for (OrderItem item : items) {
            if (done.contains(item.getOrderId() + "-" + item.getProductId())) {
                continue;
            }
            Order order = orderMap.get(item.getOrderId());
            PendingReviewVO vo = new PendingReviewVO();
            vo.setOrderId(item.getOrderId());
            vo.setOrderNo(item.getOrderNo());
            vo.setProductId(item.getProductId());
            vo.setProductName(item.getProductName());
            vo.setProductImage(item.getProductImage());
            vo.setPrice(item.getPrice());
            vo.setQuantity(item.getQuantity());
            vo.setFinishTime(order == null ? null : order.getFinishTime());
            result.add(vo);
        }
        return result;
    }

    /** 提交评价 */
    @Transactional(rollbackFor = Exception.class)
    public ReviewVO submit(Long userId, ReviewSubmitDTO dto) {
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != Order.STATUS_FINISHED) {
            throw new BusinessException("订单完成后才能评价");
        }
        OrderItem item = orderItemMapper.selectOne(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, dto.getOrderId())
                .eq(OrderItem::getProductId, dto.getProductId())
                .last("limit 1"));
        if (item == null) {
            throw new BusinessException("该订单中没有这件商品");
        }
        Long exists = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, dto.getOrderId())
                .eq(Review::getProductId, dto.getProductId()));
        if (exists != null && exists > 0) {
            throw new BusinessException("这件商品已经评价过啦");
        }

        User user = userMapper.selectById(userId);
        Review review = new Review();
        review.setUserId(userId);
        review.setUserName(user == null ? "用户" : (StringUtils.hasText(user.getNickname())
                ? user.getNickname() : user.getUsername()));
        review.setUserAvatar(user == null ? "" : user.getAvatar());
        review.setOrderId(order.getId());
        review.setOrderNo(order.getOrderNo());
        review.setMerchantId(order.getMerchantId());
        review.setProductId(item.getProductId());
        review.setProductName(item.getProductName());
        review.setProductImage(item.getProductImage());
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());
        review.setImages(dto.getImages() == null || dto.getImages().isEmpty()
                ? "" : String.join(",", dto.getImages()));
        review.setAnonymous(dto.getAnonymous() == null ? 0 : dto.getAnonymous());
        review.setCreateTime(LocalDateTime.now());
        reviewMapper.insert(review);

        refreshProductRating(item.getProductId());
        return toVO(reviewMapper.selectById(review.getId()));
    }

    /** 后台：评价分页 */
    public PageResult<ReviewVO> adminPage(String keyword, Integer rating, long pageNum, long pageSize) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(Review::getContent, keyword)
                    .or().like(Review::getProductName, keyword)
                    .or().like(Review::getUserName, keyword)
                    .or().like(Review::getOrderNo, keyword));
        }
        if (rating != null && rating > 0) {
            wrapper.eq(Review::getRating, rating);
        }
        wrapper.orderByDesc(Review::getId);
        Page<Review> page = reviewMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(),
                page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    /** 后台：回复评价 */
    // ---------------------------------------------------------------- 商家端

    /** 商家：自己店铺商品的评价 */
    public PageResult<ReviewVO> merchantPage(Long merchantId, String keyword, Integer rating, Integer replied,
                                             long pageNum, long pageSize) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>()
                .eq(Review::getMerchantId, merchantId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(Review::getContent, keyword)
                    .or().like(Review::getProductName, keyword)
                    .or().like(Review::getUserName, keyword)
                    .or().like(Review::getOrderNo, keyword));
        }
        if (rating != null && rating > 0) {
            wrapper.eq(Review::getRating, rating);
        }
        if (replied != null && replied == 0) {
            wrapper.and(q -> q.isNull(Review::getReply).or().eq(Review::getReply, ""));
        } else if (replied != null && replied == 1) {
            wrapper.isNotNull(Review::getReply).ne(Review::getReply, "");
        }
        wrapper.orderByDesc(Review::getId);
        Page<Review> page = reviewMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(),
                page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    /** 商家：回复评价 */
    public void merchantReply(Long merchantId, Long id, String reply) {
        Review review = reviewMapper.selectById(id);
        if (review == null || review.getMerchantId() == null || !review.getMerchantId().equals(merchantId)) {
            throw new BusinessException("评价不存在或不属于当前店铺");
        }
        reply(id, reply);
    }

    public void reply(Long id, String reply) {
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        if (!StringUtils.hasText(reply)) {
            throw new BusinessException("回复内容不能为空");
        }
        review.setReply(reply.trim());
        review.setReplyTime(LocalDateTime.now());
        reviewMapper.updateById(review);
    }

    /** 后台：删除评价 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        reviewMapper.deleteById(id);
        refreshProductRating(review.getProductId());
    }

    // ------------------------------------------------------------------

    /** 重算商品评分与评价数 */
    private void refreshProductRating(Long productId) {
        List<Review> list = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getProductId, productId));
        Product product = new Product();
        product.setId(productId);
        product.setReviewCount(list.size());
        if (list.isEmpty()) {
            product.setRating(new BigDecimal("5.0"));
        } else {
            int sum = 0;
            for (Review review : list) {
                sum += review.getRating() == null ? 5 : review.getRating();
            }
            product.setRating(BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(list.size()), 1, RoundingMode.HALF_UP));
        }
        productMapper.updateById(product);
    }

    private ReviewVO toVO(Review review) {
        if (review == null) {
            return null;
        }
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setUserId(review.getUserId());
        vo.setUserName(review.getUserName());
        vo.setUserAvatar(review.getUserAvatar());
        vo.setOrderId(review.getOrderId());
        vo.setOrderNo(review.getOrderNo());
        vo.setProductId(review.getProductId());
        vo.setProductName(review.getProductName());
        vo.setProductImage(review.getProductImage());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setAnonymous(review.getAnonymous());
        vo.setReply(review.getReply());
        vo.setReplyTime(review.getReplyTime());
        vo.setCreateTime(review.getCreateTime());
        if (StringUtils.hasText(review.getImages())) {
            vo.setImages(Arrays.stream(review.getImages().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList()));
        }
        // 匿名评价隐藏昵称与头像
        if (review.getAnonymous() != null && review.getAnonymous() == 1) {
            vo.setUserName("匿名用户");
            vo.setUserAvatar("");
        }
        return vo;
    }
}
