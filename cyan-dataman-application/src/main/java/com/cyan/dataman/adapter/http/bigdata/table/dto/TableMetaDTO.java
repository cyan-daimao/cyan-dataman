package com.cyan.dataman.adapter.http.bigdata.table.dto;

import com.cyan.dataman.enums.DataLayer;
import com.cyan.dataman.enums.PartitionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 表传输对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class TableMetaDTO {
    /**
     * 表名
     */
    private String name;

    /**
     * 目录
     */
    private String catalog;

    /**
     * 数据库名
     */
    private DataLayer db;

    /**
     * 表描述
     */
    private String comment;

    /**
     * 表路径
     */
    private String location;

    /**
     * 字段列表
     */
    private List<Field> fields;

    /**
     * 字段
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Accessors(chain = true)
    public static class Field {
        /**
         * 字段名
         */
        private String name;

        /**
         * 字段类型
         */
        private String type;

        /**
         * 字段是否可空
         */
        private Boolean required;

        /**
         * 字段描述
         */
        private String comment;

        /**
         * 分区
         */
        private Partition pt;

        /**
         * 分区字段
         */
        public record Partition(PartitionType type, Object parameter) {

        }


    }
}
