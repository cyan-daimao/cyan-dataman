package com.cyan.dataman.adapter.metadata.http.dto;

import com.cyan.dataman.enums.OpenStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 主题传输对象
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class MetadataSubjectDTO {

    /**
     * 主键
     */
    private String id;

    /**
     * 主题编码
     */
    private  String subjectCode;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 子主题
     */
    private List<MetadataSubjectDTO> children;
}
