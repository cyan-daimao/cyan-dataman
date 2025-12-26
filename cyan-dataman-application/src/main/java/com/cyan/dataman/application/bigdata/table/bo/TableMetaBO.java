package com.cyan.dataman.application.bigdata.table.bo;

import com.cyan.dataman.enums.DataLayer;
import com.cyan.dataman.enums.PartitionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 表业务信息
 *
 * @author cy.Y
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class TableMetaBO {

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
     * 表快照数据
     */
    private List<TableSnapshotBO> snapshots;


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
         * 字段描述
         */
        private String comment;

        /**
         * 分区
         */
        private Partition pt;

        /**
         * 分区类型
         */
        public record Partition(PartitionType type, Object parameter) {

        }
    }
}
