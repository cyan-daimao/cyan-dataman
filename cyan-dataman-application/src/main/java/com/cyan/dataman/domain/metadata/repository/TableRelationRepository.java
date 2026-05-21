package com.cyan.dataman.domain.metadata.repository;

import com.cyan.dataman.domain.metadata.TableRelation;

import java.util.List;

/**
 * 表关系仓库
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface TableRelationRepository {

    /**
     * 保存表关系
     *
     * @param relation 表关系领域对象
     * @return 保存后的表关系领域对象
     */
    TableRelation save(TableRelation relation);

    /**
     * 根据ID删除表关系
     *
     * @param id 主键
     */
    void deleteById(Long id);

    /**
     * 根据源表查询关系列表
     *
     * @param catalog 源表catalog
     * @param schema  源表schema
     * @param table   源表名
     * @return 关系列表
     */
    List<TableRelation> listBySource(String catalog, String schema, String table);

    /**
     * 根据目标表查询关系列表
     *
     * @param catalog 目标表catalog
     * @param schema  目标表schema
     * @param table   目标表名
     * @return 关系列表
     */
    List<TableRelation> listByTarget(String catalog, String schema, String table);
}
