package com.lifeshop.controller;

import com.lifeshop.common.Result;
import com.lifeshop.common.UserContext;
import com.lifeshop.dto.ConsultSendDTO;
import com.lifeshop.entity.Consult;
import com.lifeshop.service.ConsultService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 买家咨询商家（客服）
 */
@RestController
@RequestMapping("/api/consult")
public class ConsultController {

    private final ConsultService consultService;

    public ConsultController(ConsultService consultService) {
        this.consultService = consultService;
    }

    /** 我的咨询会话列表 */
    @GetMapping("/list")
    public Result<List<Consult>> list() {
        return Result.success(consultService.myList(UserContext.getUserId()));
    }

    /** 我的未读咨询数 */
    @GetMapping("/unread")
    public Result<Long> unread() {
        return Result.success(consultService.myUnread(UserContext.getUserId()));
    }

    /** 打开与某个店铺的会话（不存在则返回空会话，用于「咨询商家」弹窗） */
    @GetMapping("/with")
    public Result<Map<String, Object>> with(@RequestParam Long merchantId,
                                            @RequestParam(required = false) Long productId) {
        return Result.success(consultService.withShop(UserContext.getUserId(), merchantId, productId));
    }

    /** 会话详情（消息列表 + 未读清零） */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(consultService.detail(UserContext.getUserId(), id));
    }

    /** 发送咨询 */
    @PostMapping("/send")
    public Result<Map<String, Object>> send(@RequestBody ConsultSendDTO dto) {
        return Result.success("已发送", consultService.send(UserContext.getUserId(), dto));
    }
}
