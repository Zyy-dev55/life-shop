package com.lifeshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifeshop.common.BusinessException;
import com.lifeshop.common.PageResult;
import com.lifeshop.common.UserContext;
import com.lifeshop.dto.AfterSaleApplyDTO;
import com.lifeshop.dto.AfterSaleHandleDTO;
import com.lifeshop.dto.AfterSaleReturnDTO;
import com.lifeshop.entity.AfterSale;
import com.lifeshop.entity.Merchant;
import com.lifeshop.entity.Order;
import com.lifeshop.entity.OrderItem;
import com.lifeshop.mapper.AfterSaleMapper;
import com.lifeshop.mapper.MerchantMapper;
import com.lifeshop.mapper.OrderItemMapper;
import com.lifeshop.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后：仅退款 / 退货退款
 *
 * 状态流转：
 *   仅退款：待商家处理(0) -> 退款完成(3) / 已拒绝(4)
 *   退货退款：待商家处理(0) -> 待买家寄回(1) -> 待商家收货(2) -> 退款完成(3) / 已拒绝(4)
 *   买家可撤销(5)
 */
@Service
public class AfterSaleService {

    private final AfterSaleMapper afterSaleMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final MerchantMapper merchantMapper;

    public AfterSaleService(AfterSaleMapper afterSaleMapper, OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper, MerchantMapper merchantMapper) {
        this.afterSaleMapper = afterSaleMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.merchantMapper = merchantMapper;
    }

    /** 申请售后 */
    @Transactional(rollbackFor = Exception.class)
    public AfterSale apply(Long userId, AfterSaleApplyDTO dto) {
        OrderItem item = orderItemMapper.selectById(dto.getOrderItemId());
        if (item == null) {
            throw new BusinessException("订单商品不存在");
        }
        Order order = orderMapper.selectById(item.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        // 只有已发货或已完成的订单可申请售后
        if (order.getStatus() != Order.STATUS_SHIPPED && order.getStatus() != Order.STATUS_FINISHED) {
            throw new BusinessException("当前订单状态不可申请售后");
        }
        // 同一商品项不可重复申请（处理中）
        Long exist = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getOrderItemId, item.getId())
                .ne(AfterSale::getStatus, AfterSale.STATUS_REJECTED)
                .ne(AfterSale::getStatus, AfterSale.STATUS_CANCELED));
        if (exist != null && exist > 0) {
            throw new BusinessException("该商品已有进行中的售后申请");
        }
        if (dto.getType() == null
                || (dto.getType() != AfterSale.TYPE_REFUND_ONLY && dto.getType() != AfterSale.TYPE_RETURN_REFUND)) {
            throw new BusinessException("售后类型不正确");
        }
        // 退款金额：默认该商品实付金额（不能超过）
        BigDecimal amount = dto.getAmount() == null ? item.getTotalPrice() : dto.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            amount = item.getTotalPrice();
        }
        if (item.getTotalPrice() != null && amount.compareTo(item.getTotalPrice()) > 0) {
            amount = item.getTotalPrice();
        }

        AfterSale as = new AfterSale();
        as.setUserId(userId);
        as.setOrderId(order.getId());
        as.setOrderNo(order.getOrderNo());
        as.setOrderItemId(item.getId());
        as.setMerchantId(item.getMerchantId());
        as.setMerchantName(order.getMerchantName());
        as.setProductId(item.getProductId());
        as.setProductName(item.getProductName());
        as.setProductImage(item.getProductImage());
        as.setPrice(item.getPrice());
        as.setQuantity(item.getQuantity());
        as.setAmount(amount);
        as.setType(dto.getType());
        as.setReason(dto.getReason());
        as.setDescription(dto.getDescription());
        as.setImages(dto.getImages());
        as.setStatus(AfterSale.STATUS_WAIT_MERCHANT);
        as.setApplyTime(LocalDateTime.now());
        as.setUpdateTime(LocalDateTime.now());
        afterSaleMapper.insert(as);
        fillText(as);
        return as;
    }

    /** 用户查看自己的售后列表 */
    public PageResult<AfterSale> pageForUser(Long userId, Integer status, long pageNum, long pageSize) {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getUserId, userId);
        if (status != null) {
            wrapper.eq(AfterSale::getStatus, status);
        }
        wrapper.orderByDesc(AfterSale::getId);
        Page<AfterSale> page = afterSaleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillText(page.getRecords());
        return PageResult.of(page);
    }

    /** 用户查看售后详情 */
    public AfterSale detailForUser(Long userId, Long id) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null || !as.getUserId().equals(userId)) {
            throw new BusinessException("售后申请不存在");
        }
        fillText(as);
        return as;
    }

    /** 买家撤销申请 */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long id) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null || !as.getUserId().equals(userId)) {
            throw new BusinessException("售后申请不存在");
        }
        if (as.getStatus() != AfterSale.STATUS_WAIT_MERCHANT && as.getStatus() != AfterSale.STATUS_WAIT_RETURN) {
            throw new BusinessException("当前状态不可撤销");
        }
        afterSaleMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AfterSale>()
                .set(AfterSale::getStatus, AfterSale.STATUS_CANCELED)
                .set(AfterSale::getUpdateTime, LocalDateTime.now())
                .eq(AfterSale::getId, id));
    }

    /** 买家填写退货物流 */
    @Transactional(rollbackFor = Exception.class)
    public void returnGoods(Long userId, Long id, AfterSaleReturnDTO dto) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null || !as.getUserId().equals(userId)) {
            throw new BusinessException("售后申请不存在");
        }
        if (as.getStatus() != AfterSale.STATUS_WAIT_RETURN) {
            throw new BusinessException("当前状态不可填写退货物流");
        }
        if (dto == null || !StringUtils.hasText(dto.getTrackingNo())) {
            throw new BusinessException("请填写退货物流单号");
        }
        afterSaleMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AfterSale>()
                .set(AfterSale::getReturnTrackingNo, dto.getTrackingNo())
                .set(AfterSale::getReturnCompany, dto.getCompany())
                .set(AfterSale::getStatus, AfterSale.STATUS_WAIT_RECEIVE)
                .set(AfterSale::getReturnTime, LocalDateTime.now())
                .set(AfterSale::getUpdateTime, LocalDateTime.now())
                .eq(AfterSale::getId, id));
    }

    /** 商家处理售后（同意 / 拒绝） */
    @Transactional(rollbackFor = Exception.class)
    public void handleByMerchant(Long merchantId, Long id, AfterSaleHandleDTO dto) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null || !as.getMerchantId().equals(merchantId)) {
            throw new BusinessException("售后申请不存在");
        }
        if (as.getStatus() != AfterSale.STATUS_WAIT_MERCHANT) {
            throw new BusinessException("该申请已处理，请勿重复操作");
        }
        if (dto.getAction() == null) {
            throw new BusinessException("请选择处理方式");
        }
        if (dto.getAction() == 1) {
            // 同意：仅退款直接退款完成；退货退款进入待买家寄回
            int next = as.getType() == AfterSale.TYPE_REFUND_ONLY
                    ? AfterSale.STATUS_REFUNDED : AfterSale.STATUS_WAIT_RETURN;
            afterSaleMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AfterSale>()
                    .set(AfterSale::getStatus, next)
                    .set(AfterSale::getMerchantRemark, dto.getRemark())
                    .set(AfterSale::getMerchantHandleTime, LocalDateTime.now())
                    .set(AfterSale::getUpdateTime, LocalDateTime.now())
                    .eq(AfterSale::getId, id));
            if (next == AfterSale.STATUS_REFUNDED) {
                doRefund(as);
            }
        } else if (dto.getAction() == 2) {
            // 拒绝
            afterSaleMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AfterSale>()
                    .set(AfterSale::getStatus, AfterSale.STATUS_REJECTED)
                    .set(AfterSale::getMerchantRemark, dto.getRemark())
                    .set(AfterSale::getMerchantHandleTime, LocalDateTime.now())
                    .set(AfterSale::getUpdateTime, LocalDateTime.now())
                    .eq(AfterSale::getId, id));
        } else {
            throw new BusinessException("处理方式不正确");
        }
    }

    /** 商家确认收货并退款（退货退款场景） */
    @Transactional(rollbackFor = Exception.class)
    public void confirmRefundByMerchant(Long merchantId, Long id) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null || !as.getMerchantId().equals(merchantId)) {
            throw new BusinessException("售后申请不存在");
        }
        if (as.getStatus() != AfterSale.STATUS_WAIT_RECEIVE) {
            throw new BusinessException("当前状态不可确认退款");
        }
        afterSaleMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AfterSale>()
                .set(AfterSale::getStatus, AfterSale.STATUS_REFUNDED)
                .set(AfterSale::getUpdateTime, LocalDateTime.now())
                .eq(AfterSale::getId, id));
        doRefund(as);
    }

    /** 商家售后列表 */
    public PageResult<AfterSale> pageForMerchant(Long merchantId, Integer status, long pageNum, long pageSize) {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getMerchantId, merchantId);
        if (status != null) {
            wrapper.eq(AfterSale::getStatus, status);
        }
        wrapper.orderByDesc(AfterSale::getId);
        Page<AfterSale> page = afterSaleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillText(page.getRecords());
        return PageResult.of(page);
    }

    /** 管理员售后列表（全量，用于监管/仲裁） */
    public PageResult<AfterSale> pageForAdmin(Integer status, long pageNum, long pageSize) {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(AfterSale::getStatus, status);
        }
        wrapper.orderByDesc(AfterSale::getId);
        Page<AfterSale> page = afterSaleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillText(page.getRecords());
        return PageResult.of(page);
    }

    /** 退款执行：扣回商家余额 */
    private void doRefund(AfterSale as) {
        BigDecimal amount = as.getAmount() == null ? BigDecimal.ZERO : as.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || as.getMerchantId() == null || as.getMerchantId() == 0) {
            return;
        }
        merchantMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Merchant>()
                .setSql("balance = GREATEST(balance - " + amount + ", 0)")
                .eq(Merchant::getId, as.getMerchantId()));
        afterSaleMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AfterSale>()
                .set(AfterSale::getRefundTime, LocalDateTime.now())
                .set(AfterSale::getRefundRemark, "退款已原路退回")
                .eq(AfterSale::getId, as.getId()));
    }

    /** 填充前端展示文本 */
    private void fillText(List<AfterSale> list) {
        if (list == null) {
            return;
        }
        list.forEach(this::fillText);
    }

    private void fillText(AfterSale as) {
        if (as == null) {
            return;
        }
        as.setTypeText(as.getType() == null ? "" :
                (as.getType() == AfterSale.TYPE_REFUND_ONLY ? "仅退款" : "退货退款"));
        if (as.getStatus() == null) {
            as.setStatusText("");
            return;
        }
        switch (as.getStatus()) {
            case AfterSale.STATUS_WAIT_MERCHANT:
                as.setStatusText("待商家处理");
                break;
            case AfterSale.STATUS_WAIT_RETURN:
                as.setStatusText("待买家寄回");
                break;
            case AfterSale.STATUS_WAIT_RECEIVE:
                as.setStatusText("待商家收货");
                break;
            case AfterSale.STATUS_REFUNDED:
                as.setStatusText("退款完成");
                break;
            case AfterSale.STATUS_REJECTED:
                as.setStatusText("已拒绝");
                break;
            case AfterSale.STATUS_CANCELED:
                as.setStatusText("已撤销");
                break;
            default:
                as.setStatusText("");
        }
    }
}
