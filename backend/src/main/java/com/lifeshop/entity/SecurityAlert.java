package com.lifeshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 智能运维异常告警。由 SecOpsService 的动态基线检测算法自动生成，
 * 也可由管理员在监控看板中标记处理状态。
 */
@Data
@TableName("t_security_alert")
public class SecurityAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** QPS_SPIKE / ERROR_RATE / SLOW_RESPONSE / IP_BURST */
    private String alertType;

    /** INFO / WARN / CRITICAL */
    private String level;

    /** 触发告警的指标名 */
    private String metricName;

    /** 当前窗口观测值 */
    private BigDecimal metricValue;

    /** 动态基线值（EWMA 均值） */
    private BigDecimal baselineValue;

    /** 异常评分，越大越异常 */
    private BigDecimal score;

    private String detail;

    /** 0 未处理 / 1 已处理 */
    private Integer handled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
