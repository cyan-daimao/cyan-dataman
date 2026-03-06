package com.cyan.dataman.infra.persistence.metadata.dos;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyan.dataman.enums.OpenStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 主题表
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@TableName("metadata_subject")
public class MetadataSubjectDO {

    /**
     * 主键
     */
    @TableId("id")
    private String id;

    /**
     * 主题编码
     */
    @TableField("subject_code")
    private  String subjectCode;

    /**
     * 主题名称
     */
    @TableField("subject_name")
    private String subjectName;

    /**
     * 描述
     */
    @TableField("subject_desc")
    private String subjectDesc;

    /**
     * 父级主题id
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 层级：1 一级、2 二级、3 三级
     */
    @TableField("level")
    private int level;

    /**
     * 主题负责人
     */
    @TableField("owner")
    private String owner;

    /**
     * 开启状态
     */
    @TableField("open_status")
    private OpenStatus openStatus;

    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;

    /**
     * 修改人
     */
    @TableField("update_by")
    private String updateBy;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updateAt;

    /**
     * 逻辑删除
     */
    @TableField("deleted_at")
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}
