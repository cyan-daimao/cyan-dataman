package com.cyan.dataman.application.metadata;

import com.cyan.dataman.adapter.metadata.http.dto.TableRelationDTO;
import com.cyan.dataman.application.metadata.cmd.CreateRelationCmd;

import java.util.List;
import java.util.Map;

/**
 * 表关系服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface TableRelationService {

    /**
     * 获取表的所有关联关系（出向+入向）
     *
     * @param catalog 表 catalog
     * @param schema  表 schema
     * @param table   表名
     * @return 出向和入向关联关系
     */
    Map<String, List<TableRelationDTO>> getTableRelations(String catalog, String schema, String table);

    /**
     * 创建关联关系
     *
     * @param cmd     创建命令
     * @param createdBy 创建人
     * @return 创建后的关联关系
     */
    TableRelationDTO createRelation(CreateRelationCmd cmd, String createdBy);

    /**
     * 删除关联关系
     *
     * @param id 关联ID
     */
    void deleteRelation(Long id);

    /**
     * 批量查询 JOIN 路径
     *
     * @param factCatalog      事实表 catalog
     * @param factSchema       事实表 schema
     * @param factTable        事实表名
     * @param dimensionTables  维度表列表，每个元素为 [catalog, schema, table]
     * @return JOIN 路径列表
     */
    List<TableRelationDTO> findJoinPaths(String factCatalog, String factSchema, String factTable, List<String[]> dimensionTables);
}
