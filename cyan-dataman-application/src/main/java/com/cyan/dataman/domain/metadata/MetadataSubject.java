package com.cyan.dataman.domain.metadata;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.domain.metadata.query.MetadataSubjectFindQuery;
import com.cyan.dataman.domain.metadata.repository.MetadataSubjectRepository;
import com.cyan.dataman.enums.OpenStatus;
import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 元数据主题
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class MetadataSubject {

    /**
     * 主键
     */
    private String id;

    /**
     * 主题编码
     */
    private String subjectCode;

    /**
     * 主题名称
     */
    private String subjectName;

    /**
     * 描述
     */
    private String subjectDesc;

    /**
     * 父级主题id
     */
    private String parentId;

    /**
     * 层级：1 一级、2 二级、3 三级
     */
    private int level;

    /**
     * 主题负责人
     */
    private String owner;

    /**
     * 开启状态
     */
    private OpenStatus openStatus;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 修改人
     */
    private String updateBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updateAt;

    /**
     * 逻辑删除
     */
    private LocalDateTime deletedAt;

    /**
     * 保存
     */
    public MetadataSubject save(MetadataSubjectRepository metadataSubjectRepository) {

        Assert.isTrue(StringUtils.isNotBlank(this.id), new SilentException("不能存在主题id"));
        Assert.isTrue(StringUtils.isBlank(this.subjectCode), new SilentException("主题code不能为空"));
        Assert.isTrue(StringUtils.isBlank(this.subjectName), new SilentException("主题name不能为空"));
        Assert.isTrue(StringUtils.isBlank(this.subjectDesc), new SilentException("主题desc不能为空"));
        Assert.isTrue(StringUtils.isBlank(this.parentId), new SilentException("父id不能为空"));
        Assert.isTrue(StringUtils.isBlank(this.owner), new SilentException("负责人不能为空"));
        Assert.isTrue(openStatus == null, new SilentException("开启状态为空"));
        Assert.isTrue(StringUtils.isBlank(this.createBy), new SilentException("创建人不能为空"));
        Assert.isTrue(StringUtils.isBlank(this.updateBy), new SilentException("更新人不能为空"));
        this.createdAt = LocalDateTime.now();
        this.updateAt = LocalDateTime.now();

        if ("0".equals(this.parentId)) {
            this.level = 1;
        } else {
            MetadataSubject parent = metadataSubjectRepository.findById(this.parentId);
            this.level = parent.getLevel() + 1;
        }
        MetadataSubject metadataSubject = metadataSubjectRepository.find(new MetadataSubjectFindQuery().setSubjectCode(this.subjectCode));
        Assert.isTrue(metadataSubject.subjectCode.equals(this.subjectCode), new SilentException(metadataSubject.getSubjectCode() + "已存在"));

        metadataSubject = metadataSubjectRepository.find(new MetadataSubjectFindQuery().setSubjectName(this.subjectName));
        Assert.isTrue(metadataSubject.subjectName.equals(this.subjectName), new SilentException(metadataSubject.getSubjectName() + "已存在"));

        return metadataSubjectRepository.save(this);
    }
}
