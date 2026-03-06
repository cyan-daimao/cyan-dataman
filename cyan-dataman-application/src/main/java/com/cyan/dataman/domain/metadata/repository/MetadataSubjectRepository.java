package com.cyan.dataman.domain.metadata.repository;

import com.cyan.dataman.domain.metadata.MetadataSubject;
import com.cyan.dataman.domain.metadata.query.MetadataSubjectFindQuery;

import java.util.List;

/**
 *
 * 主题仓储
 * @author cy.Y
 * @since 1.0.0
 */
public interface MetadataSubjectRepository {

    /**
     * 根据id获取主题
     */
    MetadataSubject findById(String id);
    /**
     * 获取主题列表
     */
    List<MetadataSubject> list();

    /**
     * 保存主题
     */
    MetadataSubject save(MetadataSubject metadataSubject);

    /**
     * 更新主题
     */
    MetadataSubject update(MetadataSubject metadataSubject);

    /**
     * 删除主题
     */
    void deleteById(String id);

    /**
     * 查询主题
     *
     */
    MetadataSubject find(MetadataSubjectFindQuery query);
}
