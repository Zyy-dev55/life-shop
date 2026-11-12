package com.lifeshop.controller;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
import com.lifeshop.entity.Banner;
import com.lifeshop.entity.Category;
import com.lifeshop.entity.Product;
import com.lifeshop.service.BannerService;
import com.lifeshop.service.CategoryService;
import com.lifeshop.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台商品 / 分类 / 轮播图接口（无需登录）
 */
@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final BannerService bannerService;

    public ProductController(ProductService productService, CategoryService categoryService, BannerService bannerService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.bannerService = bannerService;
    }

    @GetMapping("/product/page")
    public Result<PageResult<Product>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long categoryId,
                                            @RequestParam(required = false) String sort,
                                            @RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "12") long pageSize) {
        return Result.success(productService.page(keyword, categoryId, sort, pageNum, pageSize));
    }

    @GetMapping("/product/hot")
    public Result<List<Product>> hot(@RequestParam(defaultValue = "8") Integer limit) {
        return Result.success(productService.hot(limit));
    }

    @GetMapping("/product/new")
    public Result<List<Product>> newest(@RequestParam(defaultValue = "8") Integer limit) {
        return Result.success(productService.newest(limit));
    }

    @GetMapping("/product/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.success(productService.detail(id));
    }

    @GetMapping("/category/list")
    public Result<List<Category>> categories() {
        return Result.success(categoryService.listEnabled());
    }

    @GetMapping("/banner/list")
    public Result<List<Banner>> banners() {
        return Result.success(bannerService.listEnabled());
    }
}
