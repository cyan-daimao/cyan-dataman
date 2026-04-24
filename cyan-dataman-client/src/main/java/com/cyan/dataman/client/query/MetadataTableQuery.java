package com.cyan.dataman.client.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 元数据表查询参数
 *
 * @author cy.Y
 * @since 1.1.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class MetadataTableQuery {

    /**
     * 搜索内容（表名或表描述）
     */
    private String content;

    /**
     * 主题编码
     */
    private String subjectCode;

    /**
     * 当前页
     */
    private Long current;

    /**
     * 页大小
     */
    private Long size;
}
