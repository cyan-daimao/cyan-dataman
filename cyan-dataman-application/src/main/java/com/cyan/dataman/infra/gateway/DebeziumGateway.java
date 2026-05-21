package com.cyan.dataman.infra.gateway;

import com.cyan.dataman.application.cdc.bo.DebeziumConnectorStatusBO;
import com.cyan.dataman.infra.dos.DebeziumDO;
import com.cyan.dataman.infra.rpc.request.ConnectorSaveRequest;
import com.cyan.dataman.infra.rpc.request.DebeziumRPC;
import com.cyan.dataman.infra.rpc.request.config.MySQLConnectorConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Debezium 网关封装层
 * <p>
 * 封装对 Debezium 服务的 Feign 调用，Application 层通过此类访问外部 CDC 能力。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class DebeziumGateway {

    private final DebeziumRPC debeziumRpc;

    /**
     * 创建连接器
     *
     * @param request 创建请求
     */
    public void createConnector(ConnectorSaveRequest request) {
        debeziumRpc.createConnector(request);
    }

    /**
     * 更新连接器配置
     *
     * @param connectorName 连接器名称
     * @param config        配置
     */
    public void updateConnector(String connectorName, MySQLConnectorConfig config) {
        debeziumRpc.updateConnector(connectorName, config);
    }

    /**
     * 查询所有连接器
     *
     * @return 连接器名称列表
     */
    public List<String> connectors() {
        return debeziumRpc.connectors();
    }

    /**
     * 删除连接器
     *
     * @param connectorName 连接器名称
     */
    public void deleteConnector(String connectorName) {
        debeziumRpc.deleteConnector(connectorName);
    }

    /**
     * 查询连接器状态
     *
     * @param connectorName 连接器名称
     * @return 连接器状态 BO
     */
    public DebeziumConnectorStatusBO connectorStatus(String connectorName) {
        DebeziumDO status = debeziumRpc.connectorStatus(connectorName);
        if (status == null) {
            return null;
        }
        DebeziumConnectorStatusBO bo = new DebeziumConnectorStatusBO();
        bo.setName(status.getName());
        bo.setType(status.getType());
        if (status.getConnector() != null) {
            DebeziumConnectorStatusBO.ConnectorStatus connector = new DebeziumConnectorStatusBO.ConnectorStatus();
            connector.setState(status.getConnector().getState());
            bo.setConnector(connector);
        }
        if (status.getTasks() != null) {
            bo.setTasks(status.getTasks().stream()
                    .map(t -> {
                        DebeziumConnectorStatusBO.TaskStatus task = new DebeziumConnectorStatusBO.TaskStatus();
                        task.setId(t.getId());
                        task.setState(t.getState());
                        task.setTrace(t.getTrace());
                        return task;
                    })
                    .toList());
        }
        return bo;
    }

    /**
     * 重启连接器
     *
     * @param connectorName 连接器名称
     */
    public void restartConnector(String connectorName) {
        debeziumRpc.restartConnector(connectorName);
    }

    /**
     * 检查 connector 是否已存在
     */
    public boolean connectorExists(String connectorName) {
        List<String> connectors = connectors();
        return connectors != null && connectors.contains(connectorName);
    }

    /**
     * 检查 connector 的 task 是否全部 RUNNING
     */
    public boolean isConnectorTasksRunning(String connectorName) {
        DebeziumConnectorStatusBO status = connectorStatus(connectorName);
        if (status == null || status.getTasks() == null || status.getTasks().isEmpty()) {
            return false;
        }
        return status.getTasks().stream()
                .allMatch(t -> "RUNNING".equals(t.getState()));
    }

    /**
     * 获取状态异常的 task 列表
     */
    public List<DebeziumConnectorStatusBO.TaskStatus> getAbnormalTasks(String connectorName) {
        DebeziumConnectorStatusBO status = connectorStatus(connectorName);
        if (status == null || status.getTasks() == null) {
            return List.of();
        }
        return status.getTasks().stream()
                .filter(t -> !"RUNNING".equals(t.getState()))
                .toList();
    }
}
