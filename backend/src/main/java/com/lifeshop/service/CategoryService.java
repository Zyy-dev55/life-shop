package com.lifeshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifeshop.common.BusinessException;
import com.lifeshop.entity.Category;
import com.lifeshop.entity.Product;
import com.lifeshop.mapper.CategoryMapper;
import com.lifeshop.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品分类服务
 */
@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    public CategoryService(CategoryMapper categoryMapper, ProductMapper productMapper) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
    }

    /** 前台：启用中的分类 */
    public List<Category> listEnabled() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    /** 后台：全部分类 */
    public List<Category> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    public Category save(Category category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new BusinessException("分类名称不能为空");
        }
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        categoryMapper.insert(category);
        return category;
    }

    public void update(Category category) {
        if (category.getId() == null) {
            throw new BusinessException("分类ID不能为空");
        }
        categoryMapper.updateById(category);
    }

    public void delete(Long id) {
        Long count = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, id));
        if (count != null && count > 0) {
            throw new BusinessException("该分类下还有 " + count + " 个商品，无法删除");
        }
        categoryMapper.deleteById(id);
    }
}
