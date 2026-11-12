package com.lifeshop.controller.admin;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
import com.lifeshop.service.ReviewService;
import com.lifeshop.vo.ReviewVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台评价管理
 */
@RestController
@RequestMapping("/api/admin/review")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/page")
    public Result<PageResult<ReviewVO>> page(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer rating,
                                             @RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(reviewService.adminPage(keyword, rating, pageNum, pageSize));
    }

    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        reviewService.reply(id, body.get("reply"));
        return Result.success("回复成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return Result.success("已删除", null);
    }
}
