package com.cyan.dataman.application.metadata;

import com.cyan.arch.common.api.Page;
import com.cyan.dataman.application.metadata.bo.MetadataTableBO;
import com.cyan.dataman.domain.metadata.query.MetadataTableListQuery;
import com.cyan.dataman.domain.metadata.query.MetadataTablePageQuery;

import java.util.List;


/**
 *
 * 元数据服务
 * @author cy.Y
 * @since 1.0.0
 */
public interface MetadataTableService {

    /**
     * 获取表列表
     */
    Page<MetadataTableBO> page(MetadataTablePageQuery query);

    /**
     * 获取表列表
     */
    List<MetadataTableBO> list(MetadataTableListQuery query);

    /**
     * 获取表
     */
    MetadataTableBO findById(String id);

}
