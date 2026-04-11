package com.cyan.dataman.infra.rpc.request;

import com.cyan.dataman.infra.rpc.request.config.ConnectorConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Debezium 连接器创建请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class ConnectorSaveRequest {
    /**
     * 连接器名称
     */
    private String name;
    /**
     * 配置信息
     */
    private ConnectorConfig config;
}
