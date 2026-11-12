package com.lifeshop.unit;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lifeshop.common.BusinessException;
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
import com.lifeshop.service.AfterSaleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L4 业务服务层回归：售后状态机 + 资金账目闭环。
 *
 * <p>状态机是本系统最容易在改动中退化的一块，这里把「合法流转」「非法流转」
 * 「越权」「重复处理」四类路径全部固化成断言。另外单独验证退款确实落在
 * 商家余额上，而不是只把状态字段改成「已退款」——这是资金闭环的关键回归点。
 */
@DisplayName("L4 售后：状态机 / 越权 / 余额闭环")
class AfterSaleStateMachineTest {

    private static final Long USER_ID = 2L;
    private static final Long MERCHANT_ID = 6L;
    private static final BigDecimal ITEM_AMOUNT = new BigDecimal("69.00");

    private AfterSaleMapper afterSaleMapper;
    private MerchantMapper merchantMapper;
    private AfterSaleService service;
    private final Map<Long, AfterSale> table = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        table.clear();
        afterSaleMapper = mock(AfterSaleMapper.class);
        OrderMapper orderMapper = mock(OrderMapper.class);
        OrderItemMapper orderItemMapper = mock(OrderItemMapper.class);
        merchantMapper = mock(MerchantMapper.class);
        service = new AfterSaleService(afterSaleMapper, orderMapper, orderItemMapper, merchantMapper);

        Order order = new Order();
        order.setId(10L);
        order.setOrderNo("LS202611120001");
        order.setUserId(USER_ID);
        order.setStatus(Order.STATUS_SHIPPED);
        order.setMerchantName("日用百货铺");
        when(orderMapper.selectById(any())).thenReturn(order);

        OrderItem item = new OrderItem();
        item.setId(20L);
        item.setOrderId(10L);
        item.setMerchantId(MERCHANT_ID);
        item.setProductId(2L);
        item.setProductName("316 不锈钢保温杯 500ml");
        item.setProductImage("/products/p2-1.jpg");
        item.setPrice(ITEM_AMOUNT);
        item.setQuantity(1);
        item.setTotalPrice(ITEM_AMOUNT);
        when(orderItemMapper.selectById(any())).thenReturn(item);

        when(afterSaleMapper.selectCount(any())).thenReturn(0L);
        when(afterSaleMapper.selectById(any())).thenAnswer(inv -> table.get(inv.getArgument(0)));
        when(afterSaleMapper.insert(any())).thenAnswer(inv -> {
            AfterSale as = inv.getArgument(0);
            as.setId(100L);
            table.put(as.getId(), as);
            return 1;
        });
        when(afterSaleMapper.update(any(), any())).thenReturn(1);
        when(merchantMapper.update(any(), any())).thenReturn(1);
    }

    private AfterSaleApplyDTO applyDTO(int type, BigDecimal amount) {
        AfterSaleApplyDTO dto = new AfterSaleApplyDTO();
        dto.setOrderItemId(20L);
        dto.setType(type);
        dto.setReason("商品与描述不符");
        dto.setDescription("回归测试自动创建");
        dto.setAmount(amount);
        return dto;
    }

    private AfterSale existing(int status, int type) {
        AfterSale as = new AfterSale();
        as.setId(100L);
        as.setUserId(USER_ID);
        as.setOrderId(10L);
        as.setOrderNo("LS202611120001");
        as.setOrderItemId(20L);
        as.setMerchantId(MERCHANT_ID);
        as.setAmount(ITEM_AMOUNT);
        as.setStatus(status);
        as.setType(type);
        table.put(100L, as);
        return as;
    }

    private AfterSaleHandleDTO handle(int action) {
        AfterSaleHandleDTO dto = new AfterSaleHandleDTO();
        dto.setAction(action);
        dto.setRemark("回归测试处理意见");
        return dto;
    }

    /** 捕获发给商家余额表的 UPDATE，断言退款金额确实落库 */
    private String capturedBalanceSql() {
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<LambdaUpdateWrapper> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(merchantMapper).update(any(), captor.capture());
        return String.valueOf(captor.getValue().getSqlSet());
    }

    // ---------------------------------------------------------------- 申请

    @Test
    @DisplayName("申请：金额不得超过商品实付金额，且返回体必须带状态/类型文案")
    void shouldCapRefundAmountAndFillDisplayText() {
        AfterSale as = service.apply(USER_ID, applyDTO(AfterSale.TYPE_REFUND_ONLY, new BigDecimal("999.00")));

        assertThat(as.getAmount()).isEqualByComparingTo(ITEM_AMOUNT);
        assertThat(as.getStatus()).isEqualTo(AfterSale.STATUS_WAIT_MERCHANT);
        assertThat(as.getMerchantId()).isEqualTo(MERCHANT_ID);
        // 回归点：早期版本 apply() 返回前漏掉 fillText，前端拿到 null 文案
        assertThat(as.getTypeText()).isEqualTo("仅退款");
        assertThat(as.getStatusText()).isEqualTo("待商家处理");
        assertThat(as.getApplyTime()).isNotNull();
    }

    @Test
    @DisplayName("申请：非法类型 / 待付款订单 / 重复申请都必须被拒绝")
    void shouldRejectInvalidApply() {
        assertThatThrownBy(() -> service.apply(USER_ID, applyDTO(9, null)))
                .isInstanceOf(BusinessException.class).hasMessage("售后类型不正确");

        // 待付款订单不可申请售后：单独构造一个订单状态为 UNPAID 的服务实例
        OrderMapper unpaidOrderMapper = mock(OrderMapper.class);
        OrderItemMapper itemMapper = mock(OrderItemMapper.class);
        Order unpaid = new Order();
        unpaid.setId(10L);
        unpaid.setUserId(USER_ID);
        unpaid.setStatus(Order.STATUS_UNPAID);
        when(unpaidOrderMapper.selectById(any())).thenReturn(unpaid);
        OrderItem item = new OrderItem();
        item.setId(20L);
        item.setOrderId(10L);
        item.setTotalPrice(ITEM_AMOUNT);
        when(itemMapper.selectById(any())).thenReturn(item);
        AfterSaleService unpaidService = new AfterSaleService(afterSaleMapper, unpaidOrderMapper,
                itemMapper, merchantMapper);
        assertThatThrownBy(() -> unpaidService.apply(USER_ID, applyDTO(AfterSale.TYPE_REFUND_ONLY, null)))
                .isInstanceOf(BusinessException.class).hasMessage("当前订单状态不可申请售后");

        when(afterSaleMapper.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(() -> service.apply(USER_ID, applyDTO(AfterSale.TYPE_REFUND_ONLY, null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已有进行中的售后申请");

        verify(afterSaleMapper, never()).insert(any());
    }

    // ---------------------------------------------------------------- 仅退款

    @Test
    @DisplayName("仅退款：商家同意 → 直接到退款完成，并扣回商家余额")
    void shouldRefundImmediatelyForRefundOnly() {
        existing(AfterSale.STATUS_WAIT_MERCHANT, AfterSale.TYPE_REFUND_ONLY);

        service.handleByMerchant(MERCHANT_ID, 100L, handle(1));

        assertThat(capturedBalanceSql())
                .contains("balance = GREATEST(balance - 69.00, 0)");
        // 断言：退款不是「只改状态」，余额表必须收到 UPDATE
        verify(merchantMapper).update(any(), any());
    }

    @Test
    @DisplayName("仅退款：商家拒绝 → 已拒绝，余额不得变动")
    void shouldNotTouchBalanceWhenRejected() {
        existing(AfterSale.STATUS_WAIT_MERCHANT, AfterSale.TYPE_REFUND_ONLY);

        service.handleByMerchant(MERCHANT_ID, 100L, handle(2));

        verify(merchantMapper, never()).update(any(), any());
    }

    // ---------------------------------------------------------------- 退货退款

    @Test
    @DisplayName("退货退款：同意 → 待买家寄回，此时不退款")
    void shouldWaitForReturnBeforeRefund() {
        existing(AfterSale.STATUS_WAIT_MERCHANT, AfterSale.TYPE_RETURN_REFUND);

        service.handleByMerchant(MERCHANT_ID, 100L, handle(1));

        verify(merchantMapper, never()).update(any(), any());
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<LambdaUpdateWrapper> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(afterSaleMapper, org.mockito.Mockito.atLeastOnce()).update(any(), captor.capture());
        assertThat(captor.getAllValues().get(0).getSqlSet()).contains("status");
    }

    @Test
    @DisplayName("退货退款：填写物流 → 待商家收货；空单号必须拒绝")
    void shouldRequireTrackingNoBeforeReceive() {
        existing(AfterSale.STATUS_WAIT_RETURN, AfterSale.TYPE_RETURN_REFUND);

        assertThatThrownBy(() -> service.returnGoods(USER_ID, 100L, new AfterSaleReturnDTO()))
                .isInstanceOf(BusinessException.class).hasMessage("请填写退货物流单号");

        AfterSaleReturnDTO dto = new AfterSaleReturnDTO();
        dto.setTrackingNo("SF1234567890");
        dto.setCompany("顺丰速运");
        service.returnGoods(USER_ID, 100L, dto);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<LambdaUpdateWrapper> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(afterSaleMapper, org.mockito.Mockito.atLeastOnce()).update(any(), captor.capture());
        assertThat(captor.getAllValues().get(0).getSqlSet())
                .contains("return_tracking_no").contains("status");
    }

    @Test
    @DisplayName("退货退款：商家确认收货 → 退款完成并扣回余额（资金闭环）")
    void shouldCompleteRefundAfterMerchantReceives() {
        existing(AfterSale.STATUS_WAIT_RECEIVE, AfterSale.TYPE_RETURN_REFUND);

        service.confirmRefundByMerchant(MERCHANT_ID, 100L);

        assertThat(capturedBalanceSql()).contains("balance = GREATEST(balance - 69.00, 0)");
    }

    // ---------------------------------------------------------------- 非法流转 / 越权

    @Test
    @DisplayName("重复处理 / 错误状态下确认退款 / 越权操作都必须被拦住")
    void shouldGuardIllegalTransitionsAndOwnership() {
        existing(AfterSale.STATUS_REFUNDED, AfterSale.TYPE_REFUND_ONLY);
        assertThatThrownBy(() -> service.handleByMerchant(MERCHANT_ID, 100L, handle(1)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已处理");

        existing(AfterSale.STATUS_WAIT_MERCHANT, AfterSale.TYPE_RETURN_REFUND);
        assertThatThrownBy(() -> service.confirmRefundByMerchant(MERCHANT_ID, 100L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不可确认退款");

        existing(AfterSale.STATUS_WAIT_MERCHANT, AfterSale.TYPE_REFUND_ONLY);
        assertThatThrownBy(() -> service.handleByMerchant(999L, 100L, handle(1)))
                .isInstanceOf(BusinessException.class).hasMessage("售后申请不存在");
        verify(merchantMapper, never()).update(any(), any());

        existing(AfterSale.STATUS_WAIT_MERCHANT, AfterSale.TYPE_REFUND_ONLY);
        assertThatThrownBy(() -> service.handleByMerchant(MERCHANT_ID, 100L, handle(9)))
                .isInstanceOf(BusinessException.class).hasMessage("处理方式不正确");
    }

    @Test
    @DisplayName("买家撤销：仅待处理 / 待寄回可撤销，退款完成后不可撤销")
    void shouldOnlyAllowCancelInEarlyStates() {
        existing(AfterSale.STATUS_WAIT_MERCHANT, AfterSale.TYPE_REFUND_ONLY);
        service.cancel(USER_ID, 100L);
        verify(afterSaleMapper).update(any(), any());

        existing(AfterSale.STATUS_REFUNDED, AfterSale.TYPE_REFUND_ONLY);
        assertThatThrownBy(() -> service.cancel(USER_ID, 100L))
                .isInstanceOf(BusinessException.class).hasMessage("当前状态不可撤销");
    }
}
