package com.cyan.dataman.client.table.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 元数据表创建请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MetadataTableCreateRequest {

    /**
     * 表名
     */
    private String name;

    /**
     * 表负责人
     */
    private String owner;

    /**
     * 主题编码
     */
    private String subjectCode;

    /**
     * 数据层级（ODS/DWD/DWS/ADS）
     */
    private String layerCode;

    /**
     * 表描述
     */
    private String comment;

    /**
     * 密级（L1/L2/L3/L4）
     */
    private String secretLevel;

    /**
     * 在线状态（ONLINE/OFFLINE）
     */
    private String onlineStatus;

    /**
     * 字段列表
     */
    private List<MetadataColumnCreateRequest> columns;
}
