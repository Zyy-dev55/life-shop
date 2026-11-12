package com.lifeshop.controller;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
import com.lifeshop.common.UserContext;
import com.lifeshop.dto.ReviewSubmitDTO;
import com.lifeshop.service.ReviewService;
import com.lifeshop.vo.PendingReviewVO;
import com.lifeshop.vo.ReviewStatsVO;
import com.lifeshop.vo.ReviewVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评价接口（商品评价列表公开，其余需要登录）
 */
@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** 商品评价：统计 + 分页列表（公开接口） */
    @GetMapping("/product/{productId}")
    public Result<Map<String, Object>> product(@PathVariable Long productId,
                                               @RequestParam(required = false) Integer rating,
                                               @RequestParam(required = false) Integer withImage,
                                               @RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "5") long pageSize) {
        ReviewStatsVO stats = reviewService.stats(productId);
        PageResult<ReviewVO> page = reviewService.pageByProduct(productId, rating, withImage, pageNum, pageSize);
        Map<String, Object> data = new HashMap<>();
        data.put("stats", stats);
        data.put("page", page);
        return Result.success(data);
    }

    /** 待评价商品 */
    @GetMapping("/pending")
    public Result<List<PendingReviewVO>> pending() {
        return Result.success(reviewService.pending(UserContext.getUserId()));
    }

    /** 我的评价 */
    @GetMapping("/my")
    public Result<PageResult<ReviewVO>> my(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(reviewService.pageByUser(UserContext.getUserId(), pageNum, pageSize));
    }

    /** 提交评价 */
    @PostMapping
    public Result<ReviewVO> submit(@RequestBody @Valid ReviewSubmitDTO dto) {
        return Result.success("评价成功", reviewService.submit(UserContext.getUserId(), dto));
    }
}
