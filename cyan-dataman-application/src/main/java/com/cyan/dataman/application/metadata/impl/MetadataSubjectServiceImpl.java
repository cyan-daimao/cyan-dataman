package com.cyan.dataman.application.metadata.impl;

import com.cyan.dataman.application.metadata.MetadataSubjectService;
import com.cyan.dataman.application.metadata.bo.MetadataSubjectBO;
import com.cyan.dataman.application.metadata.cmd.MetadataSubjectCmd;
import com.cyan.dataman.application.metadata.convert.MetadataSubjectAppConvert;
import com.cyan.dataman.domain.metadata.MetadataSubject;
import com.cyan.dataman.domain.metadata.repository.MetadataSubjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


/**
 * 元数据主题服务实现
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class MetadataSubjectServiceImpl implements MetadataSubjectService {
    private final MetadataSubjectRepository metadataSubjectRepository;

    public MetadataSubjectServiceImpl(MetadataSubjectRepository metadataSubjectRepository) {
        this.metadataSubjectRepository = metadataSubjectRepository;
    }
    /**
     * 获取主题列表
     */
    @Override
    public List<MetadataSubjectBO> list() {
        List<MetadataSubject> metadataSubjects = metadataSubjectRepository.list();
        return Optional.ofNullable(metadataSubjects).orElse(List.of()).stream().map(MetadataSubjectAppConvert.INSTANCE::toMetadataSubjectBO).toList();
    }

    /**
     * 创建主题
     */
    @Override
    public MetadataSubjectBO create(MetadataSubjectCmd cmd) {
        MetadataSubject metadataSubject = MetadataSubjectAppConvert.INSTANCE.toMetadataSubject(cmd);
        metadataSubject = metadataSubject.save(metadataSubjectRepository);
        return MetadataSubjectAppConvert.INSTANCE.toMetadataSubjectBO(metadataSubject);
    }

    /**
     * 根据id获取主题
     */
    @Override
    public MetadataSubjectBO findById(String id) {
        MetadataSubject metadataSubject = metadataSubjectRepository.findById(id);
        return MetadataSubjectAppConvert.INSTANCE.toMetadataSubjectBO(metadataSubject);
    }

    /**
     * 修改主题
     *
     * @param id 主键id
     * @param cmd 修改参数
     */
    @Override
    public MetadataSubjectBO update(String id, MetadataSubjectCmd cmd) {
        MetadataSubject metadataSubject = MetadataSubjectAppConvert.INSTANCE.toMetadataSubject(cmd);
        metadataSubject.setId(id);
        metadataSubject = metadataSubjectRepository.update(metadataSubject);
        return MetadataSubjectAppConvert.INSTANCE.toMetadataSubjectBO(metadataSubject);
    }

    /**
     * 删除主题
     *
     */
    @Override
    public void deleteById(String id) {
        metadataSubjectRepository.deleteById(id);
    }
}
