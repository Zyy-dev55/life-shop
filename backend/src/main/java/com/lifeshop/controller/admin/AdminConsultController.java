package com.lifeshop.controller.admin;

import com.lifeshop.common.PageResult;
import com.lifeshop.common.Result;
import com.lifeshop.entity.Consult;
import com.lifeshop.service.ConsultService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台 - 聊天监管（平台管理员可查看所有用户与商家的客服会话，只读）
 */
@RestController
@RequestMapping("/api/admin/consult")
public class AdminConsultController {

    private final ConsultService consultService;

    public AdminConsultController(ConsultService consultService) {
        this.consultService = consultService;
    }

    /** 全量会话分页 */
    @GetMapping("/page")
    public Result<PageResult<Consult>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(consultService.adminPage(keyword, pageNum, pageSize));
    }

    /** 会话详情（消息记录，只读不改变未读状态） */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(consultService.adminDetail(id));
    }
}
