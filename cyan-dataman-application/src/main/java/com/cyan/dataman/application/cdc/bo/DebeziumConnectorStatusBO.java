package com.cyan.dataman.application.cdc.bo;

import lombok.Data;

import java.util.List;

/**
 * Debezium 连接器状态 BO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
public class DebeziumConnectorStatusBO {

    /**
     * 连接器名称
     */
    private String name;

    /**
     * 连接器状态
     */
    private ConnectorStatus connector;

    /**
     * 任务列表
     */
    private List<TaskStatus> tasks;

    /**
     * 类型
     */
    private String type;

    /**
     * 连接器状态详情
     */
    @Data
    public static class ConnectorStatus {
        /**
         * 运行状态
         */
        private String state;
    }

    /**
     * 任务状态详情
     */
    @Data
    public static class TaskStatus {
        /**
         * 任务id
         */
        private Long id;

        /**
         * 任务状态
         */
        private String state;

        /**
         * 任务信息
         */
        private String trace;
    }
}
