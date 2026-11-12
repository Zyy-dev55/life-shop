package com.lifeshop.controller.merchant;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
import com.lifeshop.service.MerchantService;
import com.lifeshop.service.ReviewService;
import com.lifeshop.vo.ReviewVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 商家后台 - 本店商品评价
 */
@RestController
@RequestMapping("/api/merchant/review")
public class MerchantReviewController {

    private final ReviewService reviewService;
    private final MerchantService merchantService;

    public MerchantReviewController(ReviewService reviewService, MerchantService merchantService) {
        this.reviewService = reviewService;
        this.merchantService = merchantService;
    }

    @GetMapping("/page")
    public Result<PageResult<ReviewVO>> page(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer rating,
                                             @RequestParam(required = false) Integer replied,
                                             @RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(reviewService.merchantPage(
                merchantService.currentId(), keyword, rating, replied, pageNum, pageSize));
    }

    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        reviewService.merchantReply(merchantService.currentId(), id, body.get("reply"));
        return Result.success("回复成功", null);
    }
}
