package com.cyan.dataman.infra.rpc.request;

import com.cyan.dataman.infra.dos.DebeziumDO;
import com.cyan.dataman.infra.rpc.request.config.MySQLConnectorConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * debezium的restful接口
 *
 * @author cy.Y
 * @since v1.0.0
 */
@FeignClient(name = "debezium-rpc", url = "${debezium.url}")
public interface DebeziumRPC {

    /**
     * 创建连接器
     *
     * @return 创建结果
     */
    @PostMapping("/connectors")
    Object createConnector(@RequestBody ConnectorSaveRequest request);

    /**
     * 更新连接器配置
     * 注意：此接口直接接收配置对象，而非包装对象
     *
     * @return 更新结果
     */
    @PutMapping("/connectors/{connectorName}/config")
    Object updateConnector(@PathVariable("connectorName") String connectorName, @RequestBody MySQLConnectorConfig config);

    /**
     * 查询所有连接器
     *
     * @return 查询结果
     */
    @GetMapping("/connectors")
    List<String> connectors();

    /**
     * 删除连接器
     * @param name 连接器名称
     */
    @DeleteMapping("/connectors/{name}")
    Object deleteConnector(@PathVariable("name") String name);


//    ------------连接器状态-------------
    /**
     * 查询连接器状态
     * @param connectorName 连接器名称
     */
    @GetMapping("/connectors/{connectorName}/status")
    DebeziumDO connectorStatus(@PathVariable("connectorName") String connectorName);

    /**
     * 暂停连接器（标准 Kafka Connect API）
     * @param name 连接器名称
     */
    @PutMapping("/connectors/{name}/pause")
    Object pauseConnector(@PathVariable("name") String name);

    /**
     * 恢复连接器（标准 Kafka Connect API）
     * @param name 连接器名称
     */
    @PutMapping("/connectors/{name}/resume")
    Object resumeConnector(@PathVariable("name") String name);

    /**
     * 重启连接器（标准 Kafka Connect API）
     * @param name 连接器名称
     */
    @PostMapping("/connectors/{name}/restart")
    Object restartConnector(@PathVariable("name") String name);

    /**
     * 停止连接器（Kafka 3.x+ 扩展 API，可能不被旧版本支持）
     * @param name 连接器名称
     */
    @PostMapping("/connectors/{name}/stop")
    Object stopConnector(@PathVariable("name") String name);

    /**
     * 启动连接器（Kafka 3.x+ 扩展 API，可能不被旧版本支持）
     * @param name 连接器名称
     */
    @PostMapping("/connectors/{name}/start")
    Object startConnector(@PathVariable("name") String name);
}
