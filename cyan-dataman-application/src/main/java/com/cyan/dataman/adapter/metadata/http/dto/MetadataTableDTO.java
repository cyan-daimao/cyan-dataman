package com.cyan.dataman.adapter.metadata.http.dto;

import com.cyan.dataman.domain.metadata.valobj.TableValObj;
import com.cyan.dataman.enums.DatasourceType;
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
public class MetadataTableDTO {

    /**
     * 表
     */
    private TableValObj table;

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
    private DatasourceType datasourceType;

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
    private OnlineStatus onlineStatus;

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
     * 密级
     */
    private SecretLevel secretLevel;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 修改时间
     */
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;
}
