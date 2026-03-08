package com.cyan.dataman.application.metadata.cmd;

import com.cyan.dataman.enums.OpenStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 元数据主题命令
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors
public class MetadataSubjectCmd {

    /**
     * 主题编码
     */
    @NotBlank(message = "主题编码不能为空")
    private  String subjectCode;

    /**
     * 主题名称
     */
    @NotBlank(message = "主题名称不能为空")
    private String subjectName;

    /**
     * 描述
     */
    @NotBlank(message = "主题描述不能为空")
    private String subjectDesc;

    /**
     * 父级主题id
     */
    @NotBlank(message = "父级主题id不能为空")
    private String parentId;

    /**
     * 主题负责人
     */
    @NotBlank(message = "主题负责人不能为空")
    private String owner;

    /**
     * 开启状态
     */
    @NotNull(message = "主题开启状态不能为空")
    private OpenStatus openStatus;

}
