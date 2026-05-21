package com.cyan.dataman.domain.metadata;

import com.cyan.dataman.domain.metadata.repository.TableRelationRepository;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 表关系领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class TableRelation {

    /**
     * 主键
     */
    private Long id;

    /**
     * 源表 catalog
     */
    private String sourceCatalog;

    /**
     * 源表 schema
     */
    private String sourceSchema;

    /**
     * 源表名
     */
    private String sourceTable;

    /**
     * 源表字段
     */
    private String sourceColumn;

    /**
     * 目标表 catalog
     */
    private String targetCatalog;

    /**
     * 目标表 schema
     */
    private String targetSchema;

    /**
     * 目标表名
     */
    private String targetTable;

    /**
     * 目标表字段
     */
    private String targetColumn;

    /**
     * JOIN类型：LEFT/INNER/RIGHT
     */
    private String joinType;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 修改人
     */
    private String updatedBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 保存表关系
     */
    public TableRelation save(TableRelationRepository repository) {
        return repository.save(this);
    }

    /**
     * 删除表关系
     */
    public void delete(TableRelationRepository repository) {
        repository.deleteById(this.id);
    }
}
