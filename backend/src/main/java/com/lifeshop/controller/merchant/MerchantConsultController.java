package com.lifeshop.controller.merchant;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
import com.lifeshop.entity.Consult;
import com.lifeshop.service.ConsultService;
import com.lifeshop.service.MerchantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 商家后台 - 客户咨询（只处理自己店铺的会话）
 */
@RestController
@RequestMapping("/api/merchant/consult")
public class MerchantConsultController {

    private final ConsultService consultService;
    private final MerchantService merchantService;

    public MerchantConsultController(ConsultService consultService, MerchantService merchantService) {
        this.consultService = consultService;
        this.merchantService = merchantService;
    }

    /** 会话分页 */
    @GetMapping("/page")
    public Result<PageResult<Consult>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer unreadOnly,
                                            @RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(consultService.merchantPage(
                merchantService.currentId(), keyword, unreadOnly, pageNum, pageSize));
    }

    /** 未读会话数（侧边栏角标） */
    @GetMapping("/unread")
    public Result<Long> unread() {
        return Result.success(consultService.merchantUnread(merchantService.currentId()));
    }

    /** 会话详情 */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(consultService.merchantDetail(merchantService.currentId(), id));
    }

    /** 回复买家 */
    @PostMapping("/{id}/reply")
    public Result<Consult> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Consult consult = consultService.merchantReply(merchantService.currentId(), id, body.get("content"));
        return Result.success("回复成功", consult);
    }

    /** 一键已读 */
    @PostMapping("/read-all")
    public Result<Void> readAll() {
        consultService.merchantReadAll(merchantService.currentId());
        return Result.success("已全部标记为已读", null);
    }
}
