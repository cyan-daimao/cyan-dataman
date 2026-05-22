package com.cyan.dataman.client.table.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 元数据表 DTO（Client 版本）
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
     * 主题编码
     */
    private String subjectCode;

    /**
     * 数据层级
     */
    private String layerCode;

    /**
     * 描述
     */
    private String comment;

    /**
     * 在线状态
     */
    private String onlineStatus;

    /**
     * 密级
     */
    private String secretLevel;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
