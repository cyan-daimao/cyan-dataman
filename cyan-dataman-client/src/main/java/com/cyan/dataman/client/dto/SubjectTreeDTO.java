package com.cyan.dataman.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 主题树DTO
 *
 * @author cy.Y
 * @since 1.1.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class SubjectTreeDTO {

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
    private Integer level;

    /**
     * 子主题
     */
    private List<SubjectTreeDTO> children;
}
