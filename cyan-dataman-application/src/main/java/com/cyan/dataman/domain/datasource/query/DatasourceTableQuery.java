package com.cyan.dataman.domain.datasource.query;

import com.cyan.dataman.enums.StorageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotBlank(message = "数据库名称不能为空")
    private String db;

    /**
     * 存储类型
     */
    @NotNull(message = "存储类型不能为空")
    private StorageType storageType;
}
