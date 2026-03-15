package com.cyan.dataman.adapter.metadata.http.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 主题表的树
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class SubjectTableTreeDTO {

    /**
     * 节点唯一标识
     */
    private String key;

    /**
     * 节点标题
     */
    private String title;

    /**
     * 节点类型：subject-主题, table-表
     */
    private String type;

    /**
     * 主题编码（仅主题节点有值）
     */
    private String subjectCode;

    /**
     * 表ID（仅表节点有值）
     */
    private String tableId;

    /**
     * 目录（仅表节点有值）
     */
    private String catalog;

    /**
     * 模式（仅表节点有值）
     */
    private String schema;

    /**
     * 表名（仅表节点有值）
     */
    private String tableName;

    /**
     * 是否为叶子节点
     */
    private boolean isLeaf;

    /**
     * 子节点
     */
    private List<SubjectTableTreeDTO> children;
}