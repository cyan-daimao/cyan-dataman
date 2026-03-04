package com.cyan.dataman.domain.metadata.repository;

import com.cyan.arch.common.api.Page;
import com.cyan.dataman.domain.metadata.MetadataTable;
import com.cyan.dataman.domain.metadata.query.MetadataTableListQuery;
import com.cyan.dataman.domain.metadata.query.MetadataTablePageQuery;

import java.util.List;

/**
 * 元数据表仓库
 * @author cy.Y
 * @since 1.0.0
 */

public interface MetadataTableRepository {
    /**
     * 分页获取表列表
     */
    Page<MetadataTable> page(MetadataTablePageQuery query);

    /**
     * 获取表列表
     */
    List<MetadataTable> list(MetadataTableListQuery query);

    /**
     * 保存表
     */
    MetadataTable save(MetadataTable table);

    /**
     * 获取表
     */
    MetadataTable findById(String id);
}
