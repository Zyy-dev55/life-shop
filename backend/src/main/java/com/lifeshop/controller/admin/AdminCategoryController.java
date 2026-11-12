package com.lifeshop.controller.admin;

import com.lifeshop.common.Result;
import com.lifeshop.entity.Category;
import com.lifeshop.service.CategoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台 - 分类管理
 */
@RestController
@RequestMapping("/api/admin/category")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/list")
    public Result<List<Category>> list() {
        return Result.success(categoryService.listAll());
    }

    @PostMapping
    public Result<Category> save(@RequestBody Category category) {
        return Result.success("新增成功", categoryService.save(category));
    }

    @PutMapping
    public Result<Void> update(@RequestBody Category category) {
        categoryService.update(category);
        return Result.success("保存成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success("删除成功", null);
    }
}
