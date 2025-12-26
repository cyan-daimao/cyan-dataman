package com.cyan.dataman.domain.bigdata.table;

import com.cyan.dataman.domain.bigdata.table.repository.TableMetaRepository;
import com.cyan.dataman.enums.DataLayer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 表领域对象
 *
 * @author cy.Y
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class TableMeta {

    /**
     * 目录
     */
    private String catalog;

    /**
     * 表名
     */
    private String name;

    /**
     * 数据库名
     */
    private DataLayer db;

    /**
     * 全名
     */
    private String fullName;

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
     * 保存
     *
     * @param tableMetaRepository 表仓储服务
     */
    public TableMeta save(TableMetaRepository tableMetaRepository) {
        return tableMetaRepository.save(this);
    }

}
