package com.cyan.dataman.adapter.ds.http.dto;

import com.cyan.dataman.enums.DatasourceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 数据源配置DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DsConfigDTO {

    private String id;

    private String name;

    private DatasourceType datasourceType;

    private String url;

    private String username;

    private String password;

    private String description;

    private String createBy;

    private String updateBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
