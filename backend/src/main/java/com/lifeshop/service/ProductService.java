package com.lifeshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifeshop.common.BusinessException;
import com.lifeshop.common.PageResult;
import com.lifeshop.entity.Category;
import com.lifeshop.entity.Merchant;
import com.lifeshop.entity.Product;
import com.lifeshop.mapper.CategoryMapper;
import com.lifeshop.mapper.MerchantMapper;
import com.lifeshop.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品服务
 */
@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final MerchantMapper merchantMapper;

    public ProductService(ProductMapper productMapper, CategoryMapper categoryMapper, MerchantMapper merchantMapper) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.merchantMapper = merchantMapper;
    }

    /** 前台：商品分页（仅上架商品） */
    public PageResult<Product> page(String keyword, Long categoryId, String sort, long pageNum, long pageSize) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        applyCondition(wrapper, keyword, categoryId);
        if ("sales".equals(sort)) {
            wrapper.orderByDesc(Product::getSales);
        } else if ("priceAsc".equals(sort)) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("priceDesc".equals(sort)) {
            wrapper.orderByDesc(Product::getPrice);
        } else {
            wrapper.orderByDesc(Product::getId);
        }
        Page<Product> page = productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillCategoryName(page.getRecords());
        fillMerchantName(page.getRecords());
        return PageResult.of(page);
    }

    /** 后台：商品分页（含下架） */
    public PageResult<Product> adminPage(String keyword, Long categoryId, Integer status, long pageNum, long pageSize) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        applyCondition(wrapper, keyword, categoryId);
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.orderByDesc(Product::getId);
        Page<Product> page = productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillCategoryName(page.getRecords());
        fillMerchantName(page.getRecords());
        return PageResult.of(page);
    }

    private void applyCondition(LambdaQueryWrapper<Product> wrapper, String keyword, Long categoryId) {
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(Product::getName, keyword).or().like(Product::getSubtitle, keyword));
        }
        if (categoryId != null && categoryId > 0) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
    }

    /** 详情 */
    public Product detail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        Category category = categoryMapper.selectById(product.getCategoryId());
        product.setCategoryName(category == null ? "" : category.getName());
        if (product.getMerchantId() != null) {
            Merchant merchant = merchantMapper.selectById(product.getMerchantId());
            product.setMerchantName(merchant == null ? "" : merchant.getName());
            product.setMerchantLogo(merchant == null ? "" : merchant.getLogo());
        }
        return product;
    }

    /** 热销商品 */
    public List<Product> hot(Integer limit) {
        int size = (limit == null || limit <= 0) ? 8 : limit;
        Page<Product> page = productMapper.selectPage(new Page<>(1, size),
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getSales));
        return page.getRecords();
    }

    /** 新品 */
    public List<Product> newest(Integer limit) {
        int size = (limit == null || limit <= 0) ? 8 : limit;
        Page<Product> page = productMapper.selectPage(new Page<>(1, size),
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getId));
        return page.getRecords();
    }

    /** 后台：新增 */
    public Product save(Product product) {
        validate(product);
        if (product.getSales() == null) {
            product.setSales(0);
        }
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        if (product.getUnit() == null || product.getUnit().trim().isEmpty()) {
            product.setUnit("件");
        }
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        productMapper.insert(product);
        return product;
    }

    /** 后台：修改 */
    public Product update(Product product) {
        if (product.getId() == null) {
            throw new BusinessException("商品ID不能为空");
        }
        validate(product);
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
        return detail(product.getId());
    }

    /** 后台：上下架 */
    public void updateStatus(Long id, Integer status) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        product.setStatus(status);
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
    }

    /** 后台：删除 */
    public void delete(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        productMapper.deleteById(id);
    }

    // ---------------------------------------------------------------- 商家端

    /** 商家：只看自己店铺的商品 */
    public PageResult<Product> merchantPage(Long merchantId, String keyword, Long categoryId, Integer status,
                                            long pageNum, long pageSize) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getMerchantId, merchantId);
        applyCondition(wrapper, keyword, categoryId);
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.orderByDesc(Product::getId);
        Page<Product> page = productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillCategoryName(page.getRecords());
        return PageResult.of(page);
    }

    /** 商家：新增商品，店铺归属由服务端写入，避免越权 */
    public Product merchantSave(Long merchantId, Product product) {
        product.setMerchantId(merchantId);
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        return save(product);
    }

    /** 商家：修改自己的商品 */
    public Product merchantUpdate(Long merchantId, Product product) {
        if (product.getId() == null) {
            throw new BusinessException("商品ID不能为空");
        }
        Product exists = getOwnProduct(merchantId, product.getId());
        product.setMerchantId(exists.getMerchantId());
        // 销量、评分由订单和评价维护，不允许商家直接改
        product.setSales(null);
        product.setRating(null);
        product.setReviewCount(null);
        return update(product);
    }

    /** 商家：上下架自己的商品 */
    public void merchantUpdateStatus(Long merchantId, Long id, Integer status) {
        getOwnProduct(merchantId, id);
        updateStatus(id, status);
    }

    /** 商家：删除自己的商品 */
    public void merchantDelete(Long merchantId, Long id) {
        getOwnProduct(merchantId, id);
        delete(id);
    }

    /** 商家：商品详情（校验归属） */
    public Product merchantDetail(Long merchantId, Long id) {
        return getOwnProduct(merchantId, id);
    }

    private Product getOwnProduct(Long merchantId, Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getMerchantId() == null || !product.getMerchantId().equals(merchantId)) {
            throw new BusinessException("商品不存在或不属于当前店铺");
        }
        return product;
    }

    private void validate(Product product) {
        if (!StringUtils.hasText(product.getName())) {
            throw new BusinessException("商品名称不能为空");
        }
        if (product.getCategoryId() == null) {
            throw new BusinessException("请选择商品分类");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("商品售价必须大于 0");
        }
        if (product.getStock() == null || product.getStock() < 0) {
            throw new BusinessException("库存不能为负数");
        }
    }

    /** 批量填充分类名称 */
    private void fillCategoryName(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        List<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (categoryIds.isEmpty()) {
            return;
        }
        Map<Long, Category> categoryMap = categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity(), (a, b) -> a));
        for (Product product : products) {
            Category category = categoryMap.get(product.getCategoryId());
            product.setCategoryName(category == null ? "" : category.getName());
        }
    }

    /** 批量填充店铺名称 */
    private void fillMerchantName(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        List<Long> merchantIds = products.stream()
                .map(Product::getMerchantId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (merchantIds.isEmpty()) {
            return;
        }
        Map<Long, Merchant> merchantMap = merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, Function.identity(), (a, b) -> a));
        for (Product product : products) {
            Merchant merchant = merchantMap.get(product.getMerchantId());
            product.setMerchantName(merchant == null ? "" : merchant.getName());
            product.setMerchantLogo(merchant == null ? "" : merchant.getLogo());
        }
    }

    /** 供其它服务使用：分类名 */
    public List<String> listCategoryNames() {
        List<Category> categories = categoryMapper.selectList(null);
        List<String> names = new ArrayList<>();
        for (Category category : categories) {
            names.add(category.getName());
        }
        return names;
    }
}
