package com.cyan.dataman.infra.dos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Debezium 连接器状态
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DebeziumConnectorDO {
    /**
     * 运行状态
     */
    private String state;

    /**
     * 运行id
     */
    @JsonProperty("worker_id")
    private String workerId;
}
