package com.cyan.dataman.domain.metadata.query;

import com.cyan.arch.common.api.Pagination;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 元数据列表查询
 * @author cy.Y
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class MetadataTablePageQuery extends Pagination {
    /**
     * 表名
     * 支持 catalog.schema.table 搜索
     */
    private String name;
    
    /**
     * 或条件的表名字
     */
    private String orName;

    /**
     * 表描述
     */
    private String comment;
    
    /**
     * 或条件的描述
     */
    private String orComment;

    /**
     * 表拥有者
     */
    private String owner;

    /**
     * 表主题
     */
    private String subjectCode;
}
