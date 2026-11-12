package com.lifeshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifeshop.common.BusinessException;
import com.lifeshop.entity.Coupon;
import com.lifeshop.entity.UserCoupon;
import com.lifeshop.mapper.CouponMapper;
import com.lifeshop.mapper.UserCouponMapper;
import com.lifeshop.vo.UserCouponVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 优惠券服务：领券中心、我的券、结算可用券、下单锁定使用
 */
@Service
public class CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    public CouponService(CouponMapper couponMapper, UserCouponMapper userCouponMapper) {
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
    }

    /** 领券中心：展示所有上架中的券，并标记当前用户是否已领取 */
    public List<Coupon> center(Long userId) {
        List<Coupon> coupons = couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 1)
                .orderByAsc(Coupon::getId));
        if (coupons.isEmpty() || userId == null) {
            return coupons;
        }
        List<UserCoupon> mine = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId));
        Set<Long> receivedIds = mine.stream().map(UserCoupon::getCouponId).collect(Collectors.toSet());
        for (Coupon coupon : coupons) {
            coupon.setReceivedByMe(receivedIds.contains(coupon.getId()));
        }
        return coupons;
    }

    /** 领取优惠券 */
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() == null || coupon.getStatus() != 1) {
            throw new BusinessException("该优惠券已下架");
        }
        if (coupon.getTotal() != null && coupon.getReceived() != null
                && coupon.getReceived() >= coupon.getTotal()) {
            throw new BusinessException("该优惠券已被领完啦");
        }
        int limit = coupon.getLimitPerUser() == null ? 1 : coupon.getLimitPerUser();
        Long myCount = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId));
        if (myCount != null && myCount >= limit) {
            throw new BusinessException("你已经领过这张券啦，每人限领 " + limit + " 张");
        }

        int validDays = coupon.getValidDays() == null ? 30 : coupon.getValidDays();
        LocalDateTime now = LocalDateTime.now();
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());
        userCoupon.setName(coupon.getName());
        userCoupon.setType(coupon.getType());
        userCoupon.setThreshold(coupon.getThreshold());
        userCoupon.setAmount(coupon.getAmount());
        userCoupon.setStatus(0);
        userCoupon.setReceiveTime(now);
        userCoupon.setExpireTime(now.plusDays(validDays));
        userCouponMapper.insert(userCoupon);

        // 已领取数量 +1
        couponMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Coupon>()
                .setSql("received = received + 1")
                .eq(Coupon::getId, coupon.getId()));
    }

    /** 我的优惠券，status: 0 未使用 1 已使用 2 已过期 */
    public List<UserCouponVO> my(Long userId, Integer status) {
        List<UserCoupon> list = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(status != null, UserCoupon::getStatus, status)
                .orderByDesc(UserCoupon::getId));

        // 先把已过期的未使用券状态纠正为 2
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> expired = list.stream()
                .filter(c -> c.getStatus() != null && c.getStatus() == 0
                        && c.getExpireTime() != null && c.getExpireTime().isBefore(now))
                .collect(Collectors.toList());
        if (!expired.isEmpty()) {
            for (UserCoupon c : expired) {
                userCouponMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserCoupon>()
                                .set(UserCoupon::getStatus, 2)
                                .eq(UserCoupon::getId, c.getId()));
                c.setStatus(2);
            }
        }

        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 结算时可用券：未使用 + 未过期 + 满足门槛
     * @param amount 当前结算的商品总额（用于判断满减券门槛）
     */
    public List<UserCouponVO> usable(Long userId, BigDecimal amount) {
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> list = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, 0)
                .orderByDesc(UserCoupon::getId));
        List<UserCouponVO> result = new ArrayList<>();
        for (UserCoupon c : list) {
            if (c.getExpireTime() != null && c.getExpireTime().isBefore(now)) {
                continue;
            }
            boolean usable = isUsable(c, amount);
            UserCouponVO vo = toVO(c);
            vo.setUsable(usable);
            result.add(vo);
        }
        return result;
    }

    /** 判断某张券是否满足使用门槛 */
    public boolean isUsable(UserCoupon c, BigDecimal amount) {
        if (c == null) return false;
        if (amount == null) return false;
        // 无门槛券（type=2 或 threshold=0）直接可用
        if (c.getType() != null && c.getType() == 2) return true;
        if (c.getThreshold() == null || c.getThreshold().compareTo(BigDecimal.ZERO) <= 0) return true;
        return amount.compareTo(c.getThreshold()) >= 0;
    }

    /**
     * 使用优惠券：校验归属、状态、门槛，置为已使用
     * @return 抵扣金额
     */
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal use(Long userId, Long userCouponId, BigDecimal amount) {
        UserCoupon c = userCouponMapper.selectById(userCouponId);
        if (c == null || !c.getUserId().equals(userId)) {
            throw new BusinessException("优惠券不存在");
        }
        if (c.getStatus() == null || c.getStatus() != 0) {
            throw new BusinessException("该优惠券不可使用");
        }
        if (c.getExpireTime() != null && c.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("该优惠券已过期");
        }
        if (!isUsable(c, amount)) {
            throw new BusinessException("未达到使用门槛（满 " + c.getThreshold() + " 元可用）");
        }
        BigDecimal discount = c.getAmount();
        // 抵扣金额不能超过商品总额
        if (discount.compareTo(amount) > 0) {
            discount = amount;
        }
        userCouponMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserCoupon>()
                        .set(UserCoupon::getStatus, 1)
                        .set(UserCoupon::getUseTime, LocalDateTime.now())
                        .eq(UserCoupon::getId, c.getId()));
        return discount;
    }

    /** 绑定优惠券到订单（用于取消时退回） */
    public void bindOrderNo(Long userId, Long userCouponId, String orderNo) {
        if (userCouponId == null || orderNo == null) return;
        userCouponMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserCoupon>()
                        .set(UserCoupon::getOrderNo, orderNo)
                        .eq(UserCoupon::getId, userCouponId)
                        .eq(UserCoupon::getUserId, userId));
    }

    /** 取消订单时退回优惠券（订单取消后返还） */
    @Transactional(rollbackFor = Exception.class)
    public void refundByOrderNo(String orderNo) {
        if (orderNo == null) return;
        List<UserCoupon> used = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getOrderNo, orderNo)
                .eq(UserCoupon::getStatus, 1));
        for (UserCoupon c : used) {
            userCouponMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserCoupon>()
                            .set(UserCoupon::getStatus, 0)
                            .set(UserCoupon::getUseTime, null)
                            .set(UserCoupon::getOrderNo, null)
                            .eq(UserCoupon::getId, c.getId()));
        }
    }

    private UserCouponVO toVO(UserCoupon c) {
        UserCouponVO vo = new UserCouponVO();
        vo.setId(c.getId());
        vo.setCouponId(c.getCouponId());
        vo.setName(c.getName());
        vo.setType(c.getType());
        vo.setThreshold(c.getThreshold());
        vo.setAmount(c.getAmount());
        vo.setStatus(c.getStatus());
        vo.setReceiveTime(c.getReceiveTime());
        vo.setExpireTime(c.getExpireTime());
        vo.setOrderNo(c.getOrderNo());
        return vo;
    }
}
