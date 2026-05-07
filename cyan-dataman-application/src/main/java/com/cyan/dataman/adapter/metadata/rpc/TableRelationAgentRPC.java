package com.cyan.dataman.adapter.metadata.rpc;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.client.table.dto.JoinPathsRequestDTO;
import com.cyan.dataman.client.table.dto.TableRelationDTO;
import com.cyan.dataman.client.table.dto.TableRelationsResultDTO;
import com.cyan.dataman.application.metadata.TableRelationService;
import com.cyan.dataman.client.table.TableRelationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 表关系 RPC 接口（供内部服务调用，无登录拦截）
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/rpc/v1/agent/meta/table-relations")
@RequiredArgsConstructor
public class TableRelationAgentRPC implements TableRelationClient {

    private final TableRelationService tableRelationService;

    /**
     * 批量获取多张表的 JOIN 路径
     *
     * @param request 请求体
     * @return JOIN 路径列表
     */
    @PostMapping("/join-paths")
    public Response<List<TableRelationDTO>> findJoinPaths(@RequestBody JoinPathsRequestDTO request) {
        JoinPathsRequestDTO.TableRefDTO fact = request.getFactTable();
        List<JoinPathsRequestDTO.TableRefDTO> dims = request.getDimensionTables();

        List<String[]> dimTables = dims == null ? List.of() : dims.stream()
                .map(d -> new String[]{d.getCatalog(), d.getSchema(), d.getTable()})
                .toList();

        List<TableRelationDTO> paths = tableRelationService.findJoinPaths(
                fact.getCatalog(), fact.getSchema(), fact.getTable(), dimTables);
        return Response.success(paths);
    }

    /**
     * 获取指定表的所有关联关系
     *
     * @param catalog catalog
     * @param schema  schema
     * @param table   table
     * @return 出向和入向关联关系
     */
    @GetMapping("/{catalog}/{schema}/{table}/relations")
    public Response<TableRelationsResultDTO> getTableRelations(
            @PathVariable String catalog,
            @PathVariable String schema,
            @PathVariable String table) {
        Map<String, List<TableRelationDTO>> relations = tableRelationService.getTableRelations(catalog, schema, table);
        TableRelationsResultDTO result = new TableRelationsResultDTO()
                .setOutgoing(relations.get("outgoing"))
                .setIncoming(relations.get("incoming"));
        return Response.success(result);
    }
}
