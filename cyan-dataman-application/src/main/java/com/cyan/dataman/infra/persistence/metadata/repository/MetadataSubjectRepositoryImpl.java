package com.cyan.dataman.infra.persistence.metadata.repository;

import com.cyan.dataman.domain.metadata.MetadataSubject;
import com.cyan.dataman.domain.metadata.repository.MetadataSubjectRepository;
import com.cyan.dataman.infra.persistence.metadata.convert.MetadataSubjectInfraConvert;
import com.cyan.dataman.infra.persistence.metadata.dos.MetadataSubjectDO;
import com.cyan.dataman.infra.persistence.metadata.mappers.MetadataSubjectMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 *
 * 主题仓储服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class MetadataSubjectRepositoryImpl implements MetadataSubjectRepository {
    private final MetadataSubjectMapper metadataSubjectMapper;

    public MetadataSubjectRepositoryImpl(MetadataSubjectMapper metadataSubjectMapper) {
        this.metadataSubjectMapper = metadataSubjectMapper;
    }

    /**
     * 根据id获取主题
     *
     */
    @Override
    public MetadataSubject findById(String id) {
        MetadataSubjectDO metadataSubjectDO = metadataSubjectMapper.selectById(id);
        return MetadataSubjectInfraConvert.INSTANCE.toMetaDataSubject(metadataSubjectDO);
    }

    /**
     * 获取主题列表
     */
    @Override
    public List<MetadataSubject> list() {
        List<MetadataSubjectDO> metadataSubjectDOS = metadataSubjectMapper.selectList(null);
        return Optional.ofNullable(metadataSubjectDOS).orElse(List.of()).stream().map(MetadataSubjectInfraConvert.INSTANCE::toMetaDataSubject).toList();
    }

    /**
     * 保存主题
     *
     */
    @Override
    public MetadataSubject save(MetadataSubject metadataSubject) {
        MetadataSubjectDO metaDataSubjectDO = MetadataSubjectInfraConvert.INSTANCE.toMetaDataSubjectDO(metadataSubject);
        metadataSubjectMapper.insert(metaDataSubjectDO);
        return findById(metaDataSubjectDO.getId());
    }

    /**
     * 更新主题
     *
     */
    @Override
    public MetadataSubject update(MetadataSubject metadataSubject) {
        MetadataSubjectDO metaDataSubjectDO = MetadataSubjectInfraConvert.INSTANCE.toMetaDataSubjectDO(metadataSubject);
        metadataSubjectMapper.updateById(metaDataSubjectDO);
        return findById(metaDataSubjectDO.getId());
    }

    /**
     * 删除主题
     *
     */
    @Override
    public void deleteById(String id) {
        metadataSubjectMapper.deleteById(id);
    }


}
