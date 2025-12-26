package com.cyan.dataman.domain.bigdata.table.cmd;

import com.cyan.dataman.enums.FieldType;
import com.cyan.dataman.enums.PartitionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 字段命令
 *
 * @author cy.Y
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class FieldMetaCmd {

    /**
     * 字段名
     */
    @NotBlank(message = "字段名不能为空")
    private String name;

    /**
     * 字段类型
     */
    @NotNull(message = "字段类型不能为空")
    private FieldType type;

    /**
     * 是否可选
     */
    @NotNull(message = "字段是否可选不能为空")
    private Boolean required;

    /**
     * 字段描述
     */
    @NotBlank(message = "字段描述不能为空")
    private String comment;

    /**
     * 非分区字段: null
     */
    private Partition pt;

    /**
     * 分区字段
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class Partition {
        /**
         * 分区类型
         */
        private PartitionType type;

        /**
         * 分区参数
         */
        private Object parameter;
    }

}
