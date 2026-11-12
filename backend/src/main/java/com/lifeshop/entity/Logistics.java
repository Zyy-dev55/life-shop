package com.lifeshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单物流轨迹（一个订单一条物流单，多个轨迹节点）
 */
@Data
@TableName("t_logistics")
public class Logistics implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属订单 */
    private Long orderId;

    /** 订单号快照 */
    private String orderNo;

    /** 物流单号 */
    private String trackingNo;

    /** 承运公司 */
    private String company;

    /** 轨迹节点描述 */
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
