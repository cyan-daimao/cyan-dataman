package com.cyan.dataman.infra.persistence.metadata.dos;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyan.dataman.enums.HeatLevel;
import com.cyan.dataman.enums.OnlineStatus;
import com.cyan.dataman.enums.SecretLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


/**
 * 元数据表
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@TableName("metadata_table")
public class MetadataTableDO {

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 表名
     */
    @TableField(value = "name")
    private String name;

    /**
     * 负责人
     */
    @TableField(value = "owner")
    private String owner;

    /**
     * 元数据主题编码
     */
    @TableField(value = "subject_code")
    private String subjectCode;

    /**
     * 元数据主题层编码
     */
    @TableField(value = "layer_code")
    private String layerCode;

    /**
     * 描述
     */
    @TableField(value = "comment")
    private String comment;

    /**
     * 访问次数
     */
    @TableField(value = "access_count")
    private String accessCount;

    /**
     * 最后访问时间
     */
    @TableField(value = "last_access_time")
    private LocalDateTime lastAccessTime;

    /**
     * 热度等级
     */
    @TableField(value = "heat_level")
    private HeatLevel heatLevel;

    /**
     * 密级 1-4
     */
    @TableField(value = "secret_level")
    private SecretLevel secretLevel;

    /**
     * 线上状态
     */
    @TableField(value = "online_status")
    private OnlineStatus onlineStatus;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    /**
     * 删除时间
     */
    @TableField(value = "deleted_at")
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}
