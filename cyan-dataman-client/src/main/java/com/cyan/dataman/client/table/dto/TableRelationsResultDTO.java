package com.cyan.dataman.client.table.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 表关系查询结果（出向+入向）
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TableRelationsResultDTO {

    /**
     * 出向关联（本表字段 → 其他表）
     */
    private List<TableRelationDTO> outgoing;

    /**
     * 入向关联（其他表 → 本表）
     */
    private List<TableRelationDTO> incoming;
}
