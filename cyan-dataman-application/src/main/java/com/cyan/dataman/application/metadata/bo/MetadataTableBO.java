package com.cyan.dataman.application.metadata.bo;

import com.cyan.dataman.enums.HeatLevel;
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
public class MetadataTableBO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 表名
     */
    private String name;

    /**
     * 负责人
     */
    private String owner;

    /**
     * 元数据主题编码
     */
    private String subjectCode;

    /**
     * 元数据主题层编码
     */
    private String layerCode;

    /**
     * 描述
     */
    private String comment;

    /**
     * 访问次数
     */
    private String accessCount;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;

    /**
     * 热度等级
     */
    private HeatLevel heatLevel;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;
}
