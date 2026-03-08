package com.cyan.dataman.application.metadata.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.application.metadata.MetadataSubjectService;
import com.cyan.dataman.application.metadata.bo.MetadataSubjectBO;
import com.cyan.dataman.application.metadata.cmd.MetadataSubjectCmd;
import com.cyan.dataman.application.metadata.convert.MetadataSubjectAppConvert;
import com.cyan.dataman.domain.metadata.MetadataSubject;
import com.cyan.dataman.domain.metadata.query.MetadataSubjectListQuery;
import com.cyan.dataman.domain.metadata.repository.MetadataSubjectRepository;
import com.cyan.dataman.infra.util.EmployeeUtil;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 元数据主题服务实现
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class MetadataSubjectServiceImpl implements MetadataSubjectService {
    private final MetadataSubjectRepository metadataSubjectRepository;
    private final EmployeeUtil employeeUtil;

    public MetadataSubjectServiceImpl(MetadataSubjectRepository metadataSubjectRepository, EmployeeUtil employeeUtil) {
        this.metadataSubjectRepository = metadataSubjectRepository;
        this.employeeUtil = employeeUtil;
    }
    /**
     * 获取主题列表
     */
    @Override
    public List<MetadataSubjectBO> list(MetadataSubjectListQuery query) {
        List<MetadataSubject> metadataSubjects = metadataSubjectRepository.list(query);
        return metadataSubjects.stream().map(MetadataSubjectAppConvert.INSTANCE::toMetadataSubjectBO).toList();
    }

    /**
     * 创建主题
     */
    @Override
    public MetadataSubjectBO create(MetadataSubjectCmd cmd) {
        employeeUtil.validEmployee(cmd.getOwner());
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
        employeeUtil.validEmployee(cmd.getOwner());
        MetadataSubjectBO metadataSubjectBO = findById(id);
        Assert.isTrue(metadataSubjectBO==null, new SilentException("主题不存在"));
        MetadataSubject metadataSubject = MetadataSubjectAppConvert.INSTANCE.toMetadataSubject(cmd);
        metadataSubject.setCreateBy(metadataSubjectBO.getCreateBy());
        metadataSubject.setId(id);
        metadataSubject =  metadataSubject.update(metadataSubjectRepository);
        return MetadataSubjectAppConvert.INSTANCE.toMetadataSubjectBO(metadataSubject);
    }

    /**
     * 删除主题
     *
     */
    @Override
    public void deleteById(String id) {
        MetadataSubject metadataSubject = metadataSubjectRepository.findById(id);
        Assert.isTrue(metadataSubject==null, new SilentException("主题不存在"));
        metadataSubject.delete(metadataSubjectRepository);
    }

}
