package com.cyan.dataman.infra.dos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Debezium 任务状态
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DebeziumTaskDO {
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
