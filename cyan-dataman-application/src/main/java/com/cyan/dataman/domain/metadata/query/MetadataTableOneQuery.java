package com.cyan.dataman.domain.metadata.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 单查询
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class MetadataTableOneQuery {

    /**
     * 表名
     */
    private String name;

    /**
     * 数据目录
     */
    private String catalog;

    /**
     * 数据库/schema
     */
    private String schema;

    public boolean isEmpty() {
    	return name == null && catalog == null && schema == null;
    }
}
