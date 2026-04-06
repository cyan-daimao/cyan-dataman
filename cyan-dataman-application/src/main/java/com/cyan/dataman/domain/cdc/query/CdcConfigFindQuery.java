package com.cyan.dataman.domain.cdc.query;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * CDC 配置详情查询
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class CdcConfigFindQuery {

    /**
     * CDC 配置 ID
     */
    private String id;

    /**
     * CDC 配置名称
     */
    private String name;
}
