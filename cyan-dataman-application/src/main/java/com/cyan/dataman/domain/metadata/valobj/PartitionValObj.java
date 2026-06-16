package com.cyan.dataman.domain.metadata.valobj;

import com.cyan.dataman.enums.PartitionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 分区信息
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class PartitionValObj {

    /**
     * 分区字段名
     */
    @NotBlank(message = "分区字段不能为空")
    private String columnName;

    /**
     * 分区类型
     */
    @NotNull(message = "分区类型不能为空")
    private PartitionType partitionType;

    /**
     * 分区参数，BUCKET 表示桶数，TRUNCATE 表示截断长度
     */
    private Integer param;

    /**
     * 分区顺序
     */
    private Integer sortOrder;
}
