package com.cyan.dataman.domain.ds.query;

import com.cyan.dataman.enums.DatasourceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 数据源配置列表查询
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DsConfigListQuery {

    /**
     * 数据源名称（模糊查询）
     */
    private String name;

    /**
     * 数据源类型
     */
    private DatasourceType datasourceType;
}
