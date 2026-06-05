package com.cyan.dataman.adapter.metadata.quality.http.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 元数据质量运行DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class MetadataQualityRunDTO {

    /**
     * 主键
     */
    private String id;

    /**
     * 元数据表ID
     */
    private String tableId;

    /**
     * 运行状态
     */
    private String status;

    /**
     * 质量分数
     */
    private BigDecimal score;

    /**
     * 通过规则数
     */
    private Integer passCount;

    /**
     * 警告规则数
     */
    private Integer warnCount;

    /**
     * 失败规则数
     */
    private Integer failCount;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    private LocalDateTime endedAt;

    /**
     * 运行结果
     */
    private List<MetadataQualityResultDTO> results;
}
