package com.cyan.dataman.application.cdc.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Spark 任务事件
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class SparkJobEvent {

    /**
     * 事件类型
     */
    private EventType type;

    /**
     * Spark 作业配置 ID
     */
    private String sparkJobId;

    /**
     * CDC 配置 ID
     */
    private String cdcConfigId;

    /**
     * Spark 应用 ID
     */
    private String applicationId;

    /**
     * Spark SQL
     */
    private String sparkSql;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 影响行数
     */
    private Long rowsAffected;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        /**
         * 提交任务
         */
        SUBMIT,
        /**
         * 开启任务
         */
        START,
        /**
         * 关闭任务
         */
        STOP,
        /**
         * 任务成功
         */
        SUCCESS,
        /**
         * 任务失败
         */
        FAILED
    }
}
