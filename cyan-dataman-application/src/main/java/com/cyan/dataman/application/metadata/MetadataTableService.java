package com.cyan.dataman.application.metadata;

import com.cyan.arch.common.api.Page;
import com.cyan.dataman.adapter.metadata.http.dto.SubjectTableTreeDTO;
import com.cyan.dataman.application.metadata.bo.MetadataTableBO;
import com.cyan.dataman.application.metadata.cmd.MetadataTableCmd;
import com.cyan.dataman.domain.metadata.query.MetadataTableListQuery;
import com.cyan.dataman.domain.metadata.query.MetadataTableOneQuery;
import com.cyan.dataman.domain.metadata.query.MetadataTablePageQuery;

import java.util.List;


/**
 * 元数据服务
 *
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
    MetadataTableBO findOne(MetadataTableOneQuery query);

    /**
     * 获取表
     */
    MetadataTableBO findById(String id);

    /**
     * 创建表
     */
    MetadataTableBO save(MetadataTableCmd cmd);

    /**
     * 更新表
     */
    MetadataTableBO update(String id, MetadataTableCmd cmd);

    /**
     * 删除表
     */
    void delete(String id);

    /**
     * 获取主题-表树形结构
     *
     * @param content 搜索内容（表名或表描述）
     * @return 树形结构
     */
    List<SubjectTableTreeDTO> getSubjectTableTree(String content);

}
