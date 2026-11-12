package com.lifeshop.controller.admin;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
import com.lifeshop.entity.Product;
import com.lifeshop.service.ProductService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台 - 商品管理
 */
@RestController
@RequestMapping("/api/admin/product")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/page")
    public Result<PageResult<Product>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long categoryId,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(productService.adminPage(keyword, categoryId, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.success(productService.detail(id));
    }

    @PostMapping
    public Result<Product> save(@RequestBody Product product) {
        return Result.success("新增成功", productService.save(product));
    }

    @PutMapping
    public Result<Product> update(@RequestBody Product product) {
        return Result.success("保存成功", productService.update(product));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        productService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success("删除成功", null);
    }
}
