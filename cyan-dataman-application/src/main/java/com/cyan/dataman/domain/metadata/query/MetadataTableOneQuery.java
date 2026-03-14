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


    public boolean isEmpty() {
    	return name == null;
    }
}
