package com.cyan.dataman.application.cdc.sink;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.data.Record;

import java.io.Serializable;

/**
 * Iceberg 写入记录
 * <p>
 * 封装 Iceberg 表标识、数据和操作类型，用于传递给 Iceberg Sink。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class IcebergWriteRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 目标 Iceberg 表标识
     */
    private TableIdentifier tableIdentifier;

    /**
     * Iceberg 数据记录
     */
    private Record record;

    /**
     * CDC 操作类型
     */
    private String op;

    /**
     * 获取缓存键（用于分组写入）
     */
    public String getCacheKey() {
        return tableIdentifier.toString();
    }
}
