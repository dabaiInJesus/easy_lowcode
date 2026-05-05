package com.dabai.easy_lowcode.etl.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ETL任务执行日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("etl_task_log")
public class EtlTaskLog extends BaseEntity {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 执行状态 (RUNNING-运行中, SUCCESS-成功, FAILED-失败, STOPPED-停止)
     */
    private String execStatus;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 读取记录数
     */
    private Long readCount = 0L;

    /**
     * 写入记录数
     */
    private Long writeCount = 0L;

    /**
     * 跳过记录数
     */
    private Long skipCount = 0L;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行详情JSON (记录每个步骤的详细指标)
     */
    private String execDetail;
}
