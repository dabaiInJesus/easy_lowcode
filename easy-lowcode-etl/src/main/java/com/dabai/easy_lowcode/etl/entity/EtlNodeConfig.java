package com.dabai.easy_lowcode.etl.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程节点配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("etl_node_config")
public class EtlNodeConfig extends BaseEntity {

    /** 流程ID */
    private Long flowId;

    /** 前端节点ID */
    private String nodeId;

    /** 节点类型（SOURCE/TRANSFORM/TARGET） */
    private String nodeType;

    /** 节点子类型（mysql/csv/filter等） */
    private String nodeSubType;

    /** 节点名称 */
    private String nodeName;

    /** 节点参数配置JSON */
    private String configJson;

    /** 画布X坐标 */
    private Double positionX = 0.0;

    /** 画布Y坐标 */
    private Double positionY = 0.0;
}
