package com.lifeshop.unit;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lifeshop.common.BusinessException;
import com.lifeshop.dto.OrderItemDTO;
import com.lifeshop.dto.OrderSubmitDTO;
import com.lifeshop.entity.Address;
import com.lifeshop.entity.Merchant;
import com.lifeshop.entity.Order;
import com.lifeshop.entity.Product;
import com.lifeshop.mapper.AddressMapper;
import com.lifeshop.mapper.CartItemMapper;
import com.lifeshop.mapper.MerchantMapper;
import com.lifeshop.mapper.OrderItemMapper;
import com.lifeshop.mapper.OrderMapper;
import com.lifeshop.mapper.ProductMapper;
import com.lifeshop.mapper.UserMapper;
import com.lifeshop.service.CouponService;
import com.lifeshop.service.LogisticsService;
import com.lifeshop.service.OrderService;
import com.lifeshop.vo.OrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L4 业务服务层回归：按店铺拆单、运费分摊、条件更新防超卖。
 *
 * <p>这里不启动 Spring 容器，用 Mockito 把持久层替换成内存 Map，
 * 断言的粒度落在「生成的订单对象」与「下发给数据库的 SQL 片段」上：
 * 前者验证业务规则，后者验证防超卖依赖的是带条件的原子 UPDATE，而不是先查后写。
 */
@DisplayName("L4 订单服务：拆单 / 运费 / 防超卖")
class OrderSplitAndStockTest {

    private static final Long USER_ID = 2L;

    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;
    private ProductMapper productMapper;
    private MerchantMapper merchantMapper;
    private OrderService service;

    /** 模拟 t_order 自增主键，insert 后回填 id */
    private final AtomicLong sequence = new AtomicLong(100);
    private final Map<Long, Order> orderTable = new LinkedHashMap<>();
    private final Map<Long, Product> productTable = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        orderTable.clear();
        productTable.clear();
        sequence.set(100);

        orderMapper = mock(OrderMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        productMapper = mock(ProductMapper.class);
        CartItemMapper cartItemMapper = mock(CartItemMapper.class);
        AddressMapper addressMapper = mock(AddressMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        merchantMapper = mock(MerchantMapper.class);
        LogisticsService logisticsService = mock(LogisticsService.class);
        CouponService couponService = mock(CouponService.class);

        service = new OrderService(orderMapper, orderItemMapper, productMapper, cartItemMapper,
                addressMapper, userMapper, merchantMapper, logisticsService, couponService);
        // @Value 注入的业务参数在纯单元测试里手动回填
        ReflectionTestUtils.setField(service, "freight", new BigDecimal("10"));
        ReflectionTestUtils.setField(service, "freeFreightThreshold", new BigDecimal("99"));

        // 地址归属当前用户
        Address address = new Address();
        address.setId(7L);
        address.setUserId(USER_ID);
        address.setReceiver("张三");
        address.setPhone("13900000001");
        address.setProvince("浙江省");
        address.setCity("杭州市");
        address.setDistrict("余杭区");
        address.setDetail("文一西路 1 号");
        when(addressMapper.selectById(any())).thenReturn(address);

        // 商品按 id 从内存表读取
        when(productMapper.selectById(any())).thenAnswer(inv -> productTable.get(inv.getArgument(0)));
        when(productMapper.update(any(), any())).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(orderItemMapper.insert(any())).thenReturn(1);
        when(merchantMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());

        when(orderMapper.insert(any())).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(sequence.getAndIncrement());
            orderTable.put(order.getId(), order);
            return 1;
        });
        when(orderMapper.selectById(any())).thenAnswer(inv -> orderTable.get(inv.getArgument(0)));
    }

    private void putProduct(long id, long merchantId, String price, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setMerchantId(merchantId);
        p.setName("商品" + id);
        p.setPrice(new BigDecimal(price));
        p.setStock(stock);
        p.setSales(0);
        p.setStatus(1);
        productTable.put(id, p);
    }

    private OrderSubmitDTO orderOf(long... pairs) {
        OrderSubmitDTO dto = new OrderSubmitDTO();
        dto.setAddressId(7L);
        List<OrderItemDTO> items = new ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2) {
            OrderItemDTO item = new OrderItemDTO();
            item.setProductId(pairs[i]);
            item.setQuantity((int) pairs[i + 1]);
            items.add(item);
        }
        dto.setItems(items);
        return dto;
    }

    @Test
    @DisplayName("一次结算含两家店铺商品 → 拆成两笔订单，运费各自计算")
    void shouldSplitByMerchantAndChargeFreightPerOrder() {
        putProduct(1L, 1L, "60.00", 100);
        putProduct(2L, 6L, "30.00", 100);

        List<OrderVO> result = service.submit(USER_ID, orderOf(1L, 1, 2L, 1));

        // 断言 1：两笔订单，且店铺标识互不混淆
        assertThat(result).hasSize(2);
        assertThat(orderTable.values()).extracting(Order::getMerchantId)
                .containsExactlyInAnyOrder(1L, 6L);
        verify(orderMapper, org.mockito.Mockito.times(2)).insert(any());

        // 断言 2：每笔订单独立计算运费，都不满 99 元，各加 10 元
        Order first = orderTable.get(100L);
        Order second = orderTable.get(101L);
        assertThat(first.getTotalAmount()).isEqualByComparingTo("60.00");
        assertThat(first.getFreight()).isEqualByComparingTo("10");
        assertThat(first.getPayAmount()).isEqualByComparingTo("70.00");
        assertThat(second.getTotalAmount()).isEqualByComparingTo("30.00");
        assertThat(second.getFreight()).isEqualByComparingTo("10");
        assertThat(second.getPayAmount()).isEqualByComparingTo("40.00");

        // 断言 3：全部为待付款，且归属同一个买家
        assertThat(orderTable.values()).allSatisfy(o -> {
            assertThat(o.getStatus()).isEqualTo(Order.STATUS_UNPAID);
            assertThat(o.getUserId()).isEqualTo(USER_ID);
            assertThat(o.getOrderNo()).startsWith("LS");
        });
    }

    @Test
    @DisplayName("单店金额达到免运门槛 → 运费为 0，不产生多余订单")
    void shouldWaiveFreightWhenThresholdReached() {
        putProduct(3L, 3L, "100.00", 10);

        List<OrderVO> result = service.submit(USER_ID, orderOf(3L, 1));

        assertThat(result).hasSize(1);
        Order order = orderTable.get(100L);
        assertThat(order.getFreight()).isEqualByComparingTo("0");
        assertThat(order.getPayAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("库存扣减必须是带 stock>=数量 条件的原子 UPDATE")
    void shouldUseConditionalUpdateToPreventOversell() {
        putProduct(4L, 1L, "25.00", 5);

        service.submit(USER_ID, orderOf(4L, 2));

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<LambdaUpdateWrapper> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(productMapper, atLeastOnce()).update(any(), captor.capture());
        LambdaUpdateWrapper<?> wrapper = captor.getAllValues().get(0);

        // 断言 1：库存与销量在同一条语句里变更，不存在先 SELECT 再 UPDATE 的竞态窗口
        assertThat(wrapper.getSqlSet()).contains("stock = stock - 2").contains("sales = sales + 2");
        // 断言 2：条件里带下限，数据库行锁 + 条件匹配共同保证不会扣成负数
        assertThat(wrapper.getSqlSegment()).contains("stock").contains(">=");
    }

    @Test
    @DisplayName("条件更新影响 0 行（并发下被别的请求抢走库存）→ 抛业务异常且不落订单")
    void shouldRejectWhenConditionalUpdateAffectsZeroRows() {
        putProduct(5L, 1L, "25.00", 5);
        when(productMapper.update(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.submit(USER_ID, orderOf(5L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存不足");

        // 断言：库存扣减失败后绝不能继续写订单（事务回滚语义）
        verify(orderMapper, never()).insert(any());
    }

    @Test
    @DisplayName("商品已下架 → 拒绝下单，且不触发任何库存更新")
    void shouldRejectOffShelfProduct() {
        putProduct(6L, 1L, "25.00", 5);
        productTable.get(6L).setStatus(0);

        assertThatThrownBy(() -> service.submit(USER_ID, orderOf(6L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已下架");
        verify(productMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("地址不属于当前用户 → 拒绝越权下单")
    void shouldRejectForeignAddress() {
        putProduct(7L, 1L, "25.00", 5);
        Address foreign = new Address();
        foreign.setUserId(999L);
        // 覆盖 setUp 里的地址 mock
        AddressMapper mapper = mock(AddressMapper.class);
        when(mapper.selectById(any())).thenReturn(foreign);
        OrderService isolated = new OrderService(orderMapper, orderItemMapper, productMapper,
                mock(CartItemMapper.class), mapper, mock(UserMapper.class), merchantMapper,
                mock(LogisticsService.class), mock(CouponService.class));
        ReflectionTestUtils.setField(isolated, "freight", new BigDecimal("10"));
        ReflectionTestUtils.setField(isolated, "freeFreightThreshold", new BigDecimal("99"));

        assertThatThrownBy(() -> isolated.submit(USER_ID, orderOf(7L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("收货地址不存在");
        verify(orderMapper, never()).insert(any());
    }

    @Test
    @DisplayName("同店多商品只生成一笔订单，明细金额合计正确")
    void shouldMergeItemsOfSameMerchant() {
        putProduct(8L, 2L, "20.00", 10);
        putProduct(9L, 2L, "15.00", 10);

        List<OrderVO> result = service.submit(USER_ID, orderOf(8L, 2, 9L, 3));

        assertThat(result).hasSize(1);
        Order order = orderTable.get(100L);
        assertThat(order.getMerchantId()).isEqualTo(2L);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("85.00"); // 20*2 + 15*3
        assertThat(order.getPayAmount()).isEqualByComparingTo("95.00");   // 未达 99，加运费

        // 断言：两件商品都写入明细，且每笔明细金额单独核算
        ArgumentCaptor<com.lifeshop.entity.OrderItem> captor =
                ArgumentCaptor.forClass(com.lifeshop.entity.OrderItem.class);
        verify(orderItemMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(com.lifeshop.entity.OrderItem::getTotalPrice)
                .containsExactlyInAnyOrder(new BigDecimal("40.00"), new BigDecimal("45.00"));
    }
}
