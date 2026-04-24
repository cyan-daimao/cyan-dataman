package com.cyan.dataman.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 元数据表DTO
 *
 * @author cy.Y
 * @since 1.1.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class MetadataTableDTO {

    /**
     * 主键
     */
    private String id;

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
     * 数据源类型
     */
    private String datasourceType;

    /**
     * 元数据主题层编码
     */
    private String layerCode;

    /**
     * 描述
     */
    private String comment;

    /**
     * 上线状态
     */
    private String onlineStatus;

    /**
     * 访问次数
     */
    private String accessCount;

    /**
     * 最后访问时间
     */
    private String lastAccessTime;

    /**
     * 热度等级
     */
    private String heatLevel;

    /**
     * 密级
     */
    private String secretLevel;
}
