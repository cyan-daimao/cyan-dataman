package com.cyan.dataman.application.metadata;

import com.cyan.dataman.application.metadata.bo.MetadataSubjectBO;
import com.cyan.dataman.application.metadata.cmd.MetadataSubjectCmd;
import com.cyan.dataman.domain.metadata.query.MetadataSubjectListQuery;

import java.util.List;

/**
 * 元数据主题服务
 * @author cy.Y
 * @since 1.0.0
 */
public interface MetadataSubjectService {
    /**
     * 获取主题列表
     */
    List<MetadataSubjectBO> list(MetadataSubjectListQuery query);

    /**
     * 创建主题
     */
    MetadataSubjectBO create(MetadataSubjectCmd cmd);

    /**
     * 根据id获取主题
     */
    MetadataSubjectBO findById(String id);

    /**
     * 修改主题
     */
    MetadataSubjectBO update(String id, MetadataSubjectCmd cmd);

    /**
     * 删除主题
     */
    void deleteById(String id);
}
