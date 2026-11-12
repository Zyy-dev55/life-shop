package com.lifeshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifeshop.common.BusinessException;
import com.lifeshop.dto.CartAddDTO;
import com.lifeshop.dto.CartUpdateDTO;
import com.lifeshop.entity.CartItem;
import com.lifeshop.entity.Merchant;
import com.lifeshop.entity.Product;
import com.lifeshop.mapper.CartItemMapper;
import com.lifeshop.mapper.MerchantMapper;
import com.lifeshop.mapper.ProductMapper;
import com.lifeshop.vo.CartVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车服务
 */
@Service
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final MerchantMapper merchantMapper;

    public CartService(CartItemMapper cartItemMapper, ProductMapper productMapper, MerchantMapper merchantMapper) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.merchantMapper = merchantMapper;
    }

    /** 购物车列表（关联商品信息） */
    public List<CartVO> list(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getId));
        if (items.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> productIds = items.stream().map(CartItem::getProductId).distinct().collect(Collectors.toList());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity(), (a, b) -> a));

        // 店铺名称，结算页按店铺分组展示
        List<Long> merchantIds = productMap.values().stream()
                .map(Product::getMerchantId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> merchantNames = merchantIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : merchantMapper.selectBatchIds(merchantIds).stream()
                    .collect(Collectors.toMap(Merchant::getId, Merchant::getName, (a, b) -> a));

        List<CartVO> result = new ArrayList<>();
        for (CartItem item : items) {
            Product product = productMap.get(item.getProductId());
            if (product == null) {
                // 商品已被删除，清理无效购物车项
                cartItemMapper.deleteById(item.getId());
                continue;
            }
            CartVO vo = new CartVO();
            vo.setId(item.getId());
            vo.setProductId(product.getId());
            vo.setMerchantId(product.getMerchantId());
            vo.setMerchantName(merchantNames.get(product.getMerchantId()));
            vo.setName(product.getName());
            vo.setMainImage(product.getMainImage());
            vo.setUnit(product.getUnit());
            vo.setPrice(product.getPrice());
            vo.setStock(product.getStock());
            vo.setQuantity(item.getQuantity());
            vo.setChecked(item.getChecked());
            vo.setStatus(product.getStatus());
            vo.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            result.add(vo);
        }
        return result;
    }

    /** 加入购物车 */
    public void add(Long userId, CartAddDTO dto) {
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException("商品不存在或已下架");
        }
        CartItem exists = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, dto.getProductId()));
        int quantity = dto.getQuantity();
        if (exists != null) {
            quantity = exists.getQuantity() + dto.getQuantity();
        }
        if (product.getStock() != null && quantity > product.getStock()) {
            throw new BusinessException("库存不足，当前仅剩 " + product.getStock() + " " + product.getUnit());
        }
        if (exists != null) {
            exists.setQuantity(quantity);
            exists.setChecked(1);
            cartItemMapper.updateById(exists);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(dto.getProductId());
            item.setQuantity(quantity);
            item.setChecked(1);
            cartItemMapper.insert(item);
        }
    }

    /** 修改数量 */
    public void updateQuantity(Long userId, CartUpdateDTO dto) {
        CartItem item = getOwnItem(userId, dto.getId());
        Product product = productMapper.selectById(item.getProductId());
        if (product != null && product.getStock() != null && dto.getQuantity() > product.getStock()) {
            throw new BusinessException("库存不足，当前仅剩 " + product.getStock() + " " + product.getUnit());
        }
        item.setQuantity(dto.getQuantity());
        cartItemMapper.updateById(item);
    }

    /** 勾选 / 取消勾选单项 */
    public void updateChecked(Long userId, Long id, Integer checked) {
        CartItem item = getOwnItem(userId, id);
        item.setChecked(checked != null && checked == 1 ? 1 : 0);
        cartItemMapper.updateById(item);
    }

    /** 全选 / 全不选 */
    public void checkAll(Long userId, Integer checked) {
        List<CartItem> items = cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
        for (CartItem item : items) {
            item.setChecked(checked != null && checked == 1 ? 1 : 0);
            cartItemMapper.updateById(item);
        }
    }

    /** 删除 */
    public void remove(Long userId, Long id) {
        getOwnItem(userId, id);
        cartItemMapper.deleteById(id);
    }

    /** 清空 */
    public void clear(Long userId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
    }

    /** 购物车商品种类数 */
    public Long count(Long userId) {
        return cartItemMapper.selectCount(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
    }

    private CartItem getOwnItem(Long userId, Long id) {
        CartItem item = cartItemMapper.selectById(id);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException("购物车项不存在");
        }
        return item;
    }
}
