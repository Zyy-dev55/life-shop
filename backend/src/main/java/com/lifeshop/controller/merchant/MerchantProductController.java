package com.lifeshop.controller.merchant;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
import com.lifeshop.entity.Product;
import com.lifeshop.service.MerchantService;
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
 * 商家后台 - 本店商品管理（所有接口都按当前店铺过滤）
 */
@RestController
@RequestMapping("/api/merchant/product")
public class MerchantProductController {

    private final ProductService productService;
    private final MerchantService merchantService;

    public MerchantProductController(ProductService productService, MerchantService merchantService) {
        this.productService = productService;
        this.merchantService = merchantService;
    }

    @GetMapping("/page")
    public Result<PageResult<Product>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long categoryId,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(productService.merchantPage(
                merchantService.currentId(), keyword, categoryId, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.success(productService.merchantDetail(merchantService.currentId(), id));
    }

    @PostMapping
    public Result<Product> save(@RequestBody Product product) {
        return Result.success("商品已发布", productService.merchantSave(merchantService.currentId(), product));
    }

    @PutMapping
    public Result<Product> update(@RequestBody Product product) {
        return Result.success("商品已保存", productService.merchantUpdate(merchantService.currentId(), product));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        productService.merchantUpdateStatus(merchantService.currentId(), id, status);
        return Result.success(status == 1 ? "商品已上架" : "商品已下架", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.merchantDelete(merchantService.currentId(), id);
        return Result.success("商品已删除", null);
    }
}
