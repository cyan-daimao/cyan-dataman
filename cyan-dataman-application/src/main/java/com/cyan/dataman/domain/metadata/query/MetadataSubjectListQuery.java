package com.cyan.dataman.domain.metadata.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 元数据主题列表查询
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class MetadataSubjectListQuery {
    /**
     * 父级主题id
     */
    private String parentId;
}
