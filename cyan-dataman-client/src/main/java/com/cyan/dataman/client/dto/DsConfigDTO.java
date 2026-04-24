package com.cyan.dataman.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 数据源配置DTO
 *
 * @author cy.Y
 * @since 1.1.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DsConfigDTO {

    /**
     * 主键
     */
    private String id;

    /**
     * 数据源名称
     */
    private String name;

    /**
     * 数据源类型
     */
    private String datasourceType;

    /**
     * 连接URL
     */
    private String url;

    /**
     * 用户名
     */
    private String username;

    /**
     * 描述
     */
    private String description;
}
