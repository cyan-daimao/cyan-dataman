package com.cyan.dataman.client;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.client.dto.DsConfigDTO;
import com.cyan.dataman.client.dto.SqlExecuteCmd;
import com.cyan.dataman.client.dto.SqlResultDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 数据源 Feign Client
 *
 * @author cy.Y
 * @since 1.1.0
 */
@FeignClient(name = "cyan-dataman", contextId = "datamanDsClient", path = "/api/v1/ds")
public interface DatamanDsClient {

    /**
     * 获取数据源配置列表
     */
    @GetMapping
    Response<List<DsConfigDTO>> listDsConfigs(); // API: ready

    /**
     * 获取数据源配置详情
     */
    @GetMapping("/{dsName}")
    Response<DsConfigDTO> getDsConfig(@PathVariable("dsName") String dsName); // API: ready

    /**
     * 执行SQL
     */
    @PostMapping("/{dsName}/dbs/{db}/execute")
    Response<SqlResultDTO> executeSql(
            @PathVariable("dsName") String dsName,
            @PathVariable("db") String db,
            @RequestBody SqlExecuteCmd cmd
    ); // API: ready
}
