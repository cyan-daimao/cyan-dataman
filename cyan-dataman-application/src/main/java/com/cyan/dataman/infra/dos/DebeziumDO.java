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

    private String name;
    private DebeziumConnectorDO connector;
    private List<DebeziumTaskDO> tasks;
    private String type;
}
