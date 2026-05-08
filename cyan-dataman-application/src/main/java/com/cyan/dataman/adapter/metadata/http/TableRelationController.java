package com.cyan.dataman.adapter.metadata.http;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.client.table.dto.JoinPathsRequestDTO;
import com.cyan.dataman.client.table.dto.TableRelationDTO;
import com.cyan.dataman.client.table.dto.TableRelationsResultDTO;
import com.cyan.dataman.application.metadata.TableRelationService;
import com.cyan.dataman.application.metadata.cmd.CreateRelationCmd;
import com.cyan.employee.login.filter.UserContextHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 表关系控制器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/metadata/tables")
public class TableRelationController {

    private final TableRelationService tableRelationService;

    public TableRelationController(TableRelationService tableRelationService) {
        this.tableRelationService = tableRelationService;
    }

    /**
     * 获取表的所有关联关系
     *
     * @param catalog 表 catalog
     * @param schema  表 schema
     * @param table   表名
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

    /**
     * 创建关联关系
     *
     * @param cmd 创建命令
     * @return 创建后的关联关系
     */
    @PostMapping("/relations")
    public Response<TableRelationDTO> createRelation(@RequestBody @Valid CreateRelationCmd cmd) {
        String createdBy = UserContextHolder.getCurrentEmployee().getPassport();
        TableRelationDTO relation = tableRelationService.createRelation(cmd, createdBy);
        return Response.success(relation);
    }

    /**
     * 删除关联关系
     *
     * @param id 关联ID
     * @return 空响应
     */
    @DeleteMapping("/relations/{id}")
    public Response<Void> deleteRelation(@PathVariable Long id) {
        tableRelationService.deleteRelation(id);
        return Response.success();
    }

    /**
     * 批量获取多张表的 JOIN 路径（供指标平台调用）
     *
     * @param request 请求体
     * @return JOIN 路径列表
     */
    @PostMapping("/relations/join-paths")
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
}
