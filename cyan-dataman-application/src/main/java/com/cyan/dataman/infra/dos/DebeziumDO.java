package com.cyan.dataman.infra.dos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Debezium 状态响应
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DebeziumDO {

    /**
     * 连接器名称
     */
    private String name;

    /**
     * 连接器状态
     */
    private DebeziumConnectorDO connector;

    /**
     * 任务列表
     */
    private List<DebeziumTaskDO> tasks;

    /**
     * type: source
     * 不知道是什么
     */
    private String type;
}