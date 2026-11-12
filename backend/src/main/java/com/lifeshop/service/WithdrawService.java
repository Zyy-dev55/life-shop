package com.lifeshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifeshop.common.BusinessException;
import com.lifeshop.common.PageResult;
import com.lifeshop.common.UserContext;
import com.lifeshop.dto.WithdrawApplyDTO;
import com.lifeshop.dto.WithdrawAuditDTO;
import com.lifeshop.entity.Merchant;
import com.lifeshop.entity.Order;
import com.lifeshop.entity.Withdraw;
import com.lifeshop.mapper.MerchantMapper;
import com.lifeshop.mapper.OrderMapper;
import com.lifeshop.mapper.WithdrawMapper;
import com.lifeshop.vo.WalletVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商家提现：钱包概览、发起提现、审核
 */
@Service
public class WithdrawService {

    private final WithdrawMapper withdrawMapper;
    private final MerchantMapper merchantMapper;
    private final OrderMapper orderMapper;

    public WithdrawService(WithdrawMapper withdrawMapper, MerchantMapper merchantMapper, OrderMapper orderMapper) {
        this.withdrawMapper = withdrawMapper;
        this.merchantMapper = merchantMapper;
        this.orderMapper = orderMapper;
    }

    /** 钱包概览 */
    public WalletVO wallet(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        BigDecimal balance = merchant == null || merchant.getBalance() == null
                ? BigDecimal.ZERO : merchant.getBalance();

        // 未入账：已支付但未完成的订单货款（待发货 + 待收货）
        List<Order> pending = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId)
                .in(Order::getStatus, Order.STATUS_UNSHIPPED, Order.STATUS_SHIPPED));
        BigDecimal unsettled = pending.stream()
                .map(o -> o.getPayAmount() == null ? BigDecimal.ZERO : o.getPayAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 审核中的提现（已冻结）
        List<Withdraw> pendingWithdraws = withdrawMapper.selectList(new LambdaQueryWrapper<Withdraw>()
                .eq(Withdraw::getMerchantId, merchantId)
                .eq(Withdraw::getStatus, Withdraw.STATUS_PENDING));
        BigDecimal pendingWithdraw = pendingWithdraws.stream()
                .map(w -> w.getAmount() == null ? BigDecimal.ZERO : w.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 累计已提现（已打款）
        List<Withdraw> approved = withdrawMapper.selectList(new LambdaQueryWrapper<Withdraw>()
                .eq(Withdraw::getMerchantId, merchantId)
                .eq(Withdraw::getStatus, Withdraw.STATUS_APPROVED));
        BigDecimal totalWithdrawn = approved.stream()
                .map(w -> w.getAmount() == null ? BigDecimal.ZERO : w.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        WalletVO vo = new WalletVO();
        vo.setBalance(balance);
        vo.setUnsettled(unsettled);
        vo.setPendingWithdraw(pendingWithdraw);
        vo.setTotalWithdrawn(totalWithdrawn);
        return vo;
    }

    /** 发起提现 */
    @Transactional(rollbackFor = Exception.class)
    public void apply(Long merchantId, WithdrawApplyDTO dto) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("店铺不存在");
        }
        BigDecimal balance = merchant.getBalance() == null ? BigDecimal.ZERO : merchant.getBalance();
        BigDecimal amount = dto.getAmount();
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException("可提现余额不足");
        }
        // 银行卡需要持卡人姓名
        if (dto.getMethod() != null && dto.getMethod() == 1 && !StringUtils.hasText(dto.getAccountName())) {
            throw new BusinessException("请输入持卡人姓名");
        }

        // 扣减余额
        int rows = merchantMapper.update(null, new LambdaUpdateWrapper<Merchant>()
                .setSql("balance = balance - " + amount)
                .eq(Merchant::getId, merchantId)
                .ge(Merchant::getBalance, amount));
        if (rows <= 0) {
            throw new BusinessException("可提现余额不足");
        }

        Withdraw withdraw = new Withdraw();
        withdraw.setMerchantId(merchantId);
        withdraw.setMerchantName(merchant.getName());
        withdraw.setAmount(amount);
        withdraw.setMethod(dto.getMethod());
        withdraw.setAccountNo(dto.getAccountNo());
        withdraw.setAccountName(dto.getAccountName());
        withdraw.setStatus(Withdraw.STATUS_PENDING);
        withdraw.setApplyTime(LocalDateTime.now());
        withdrawMapper.insert(withdraw);
    }

    /** 商家提现记录分页 */
    public PageResult<Withdraw> merchantPage(Long merchantId, Integer status, long pageNum, long pageSize) {
        LambdaQueryWrapper<Withdraw> wrapper = new LambdaQueryWrapper<Withdraw>()
                .eq(Withdraw::getMerchantId, merchantId)
                .eq(status != null, Withdraw::getStatus, status)
                .orderByDesc(Withdraw::getId);
        Page<Withdraw> page = withdrawMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        page.getRecords().forEach(this::fillMethodText);
        return PageResult.of(page);
    }

    /** 管理员：提现审核列表 */
    public PageResult<Withdraw> adminPage(Integer status, String keyword, long pageNum, long pageSize) {
        LambdaQueryWrapper<Withdraw> wrapper = new LambdaQueryWrapper<Withdraw>()
                .eq(status != null, Withdraw::getStatus, status)
                .and(StringUtils.hasText(keyword), q -> q
                        .like(Withdraw::getMerchantName, keyword)
                        .or().like(Withdraw::getAccountNo, keyword))
                .orderByAsc(Withdraw::getStatus)
                .orderByDesc(Withdraw::getId);
        Page<Withdraw> page = withdrawMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        page.getRecords().forEach(this::fillMethodText);
        return PageResult.of(page);
    }

    /** 管理员审核 */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long withdrawId, WithdrawAuditDTO dto) {
        Withdraw withdraw = withdrawMapper.selectById(withdrawId);
        if (withdraw == null) {
            throw new BusinessException("提现记录不存在");
        }
        if (withdraw.getStatus() == null || withdraw.getStatus() != Withdraw.STATUS_PENDING) {
            throw new BusinessException("该提现已处理，请勿重复操作");
        }
        if (dto.getStatus() == Withdraw.STATUS_REJECTED && !StringUtils.hasText(dto.getRemark())) {
            throw new BusinessException("请填写驳回原因");
        }

        withdraw.setStatus(dto.getStatus());
        withdraw.setAuditRemark(dto.getRemark());
        withdraw.setAuditTime(LocalDateTime.now());
        withdraw.setAuditBy(UserContext.get().getUsername());
        withdrawMapper.updateById(withdraw);

        // 驳回：退回余额
        if (dto.getStatus() == Withdraw.STATUS_REJECTED) {
            merchantMapper.update(null, new LambdaUpdateWrapper<Merchant>()
                    .setSql("balance = balance + " + withdraw.getAmount())
                    .eq(Merchant::getId, withdraw.getMerchantId()));
        }
    }

    private void fillMethodText(Withdraw w) {
        if (w.getMethod() == null) {
            w.setMethodText("未知");
            return;
        }
        switch (w.getMethod()) {
            case 1:
                w.setMethodText("银行卡");
                break;
            case 2:
                w.setMethodText("微信");
                break;
            case 3:
                w.setMethodText("支付宝");
                break;
            default:
                w.setMethodText("未知");
        }
    }
}
