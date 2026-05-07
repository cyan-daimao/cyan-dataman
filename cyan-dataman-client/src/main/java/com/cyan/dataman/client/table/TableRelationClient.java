package com.cyan.dataman.client.table;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.client.table.dto.JoinPathsRequestDTO;
import com.cyan.dataman.client.table.dto.TableRelationDTO;
import com.cyan.dataman.client.table.dto.TableRelationsResultDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 元数据平台表关系 Feign 客户端
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "cyan-dataman", path = "/rpc/v1/agent/meta/table-relations")
public interface TableRelationClient {

    /**
     * 查询事实表到维度表的 JOIN 路径
     *
     * @param request 请求体
     * @return JOIN 关系列表
     */
    @PostMapping("/join-paths")
    Response<List<TableRelationDTO>> findJoinPaths(@RequestBody JoinPathsRequestDTO request);

    /**
     * 获取指定表的所有关联关系
     *
     * @param catalog catalog
     * @param schema  schema
     * @param table   table
     * @return 关联关系（出向+入向）
     */
    @GetMapping("/{catalog}/{schema}/{table}/relations")
    Response<TableRelationsResultDTO> getTableRelations(@PathVariable String catalog,
                                                         @PathVariable String schema,
                                                         @PathVariable String table);

    /**
     * 便捷方法：扁平化参数查询 JOIN 路径
     *
     * @param factCatalog  事实表 catalog
     * @param factSchema   事实表 schema
     * @param factTable    事实表名
     * @param dimTableRefs 维度表列表（格式：catalog.schema.table）
     * @return JOIN 关系列表
     */
    default List<TableRelationDTO> findJoinPaths(String factCatalog, String factSchema, String factTable,
                                                  List<String> dimTableRefs) {
        JoinPathsRequestDTO request = new JoinPathsRequestDTO()
                .setFactTable(new JoinPathsRequestDTO.TableRefDTO(factCatalog, factSchema, factTable))
                .setDimensionTables(dimTableRefs.stream()
                        .map(ref -> {
                            String[] parts = ref.split("\\.");
                            if (parts.length != 3) {
                                throw new IllegalArgumentException(
                                        "维度表引用格式错误，期望 catalog.schema.table 或 schema.table，实际: " + ref);
                            }
                            return new JoinPathsRequestDTO.TableRefDTO(parts[0], parts[1], parts[2]);
                        })
                        .toList());
        Response<List<TableRelationDTO>> response = findJoinPaths(request);
        if (response == null || response.getData() == null) {
            return List.of();
        }
        return response.getData();
    }
}
