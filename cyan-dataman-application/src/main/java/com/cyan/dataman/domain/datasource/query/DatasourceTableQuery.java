package com.cyan.dataman.domain.datasource.query;

import com.cyan.dataman.enums.StorageType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 数据源表参数参数
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DatasourceTableQuery {

    /**
     * 数据库名称
     */
    private String db;

    /**
     * 表名称
     */
    private String name;

    /**
     * 存储类型
     */
    @NotBlank
    private StorageType storageType;
}
