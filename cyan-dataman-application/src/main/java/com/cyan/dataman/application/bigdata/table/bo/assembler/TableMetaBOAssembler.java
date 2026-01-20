package com.cyan.dataman.application.bigdata.table.bo.assembler;

import com.cyan.dataman.application.bigdata.table.bo.TableMetaBO;
import com.cyan.dataman.application.bigdata.table.bo.TableSnapshotBO;
import com.cyan.dataman.application.bigdata.table.convert.TableMetaAppConvert;
import com.cyan.dataman.domain.bigdata.table.TableSnapshot;
import com.cyan.dataman.domain.bigdata.table.repository.TableMetaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 表元数据组装器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
public class TableMetaBOAssembler {
    private final TableMetaRepository tableMetaRepository;

    public TableMetaBOAssembler(TableMetaRepository tableMetaRepository) {
        this.tableMetaRepository = tableMetaRepository;
    }

    public void assemblerSnapshots(TableMetaBO tableMetaBO) {
        List<TableSnapshot> snapshots = tableMetaRepository.snapshots(tableMetaBO.getDb(), tableMetaBO.getTbl());
        List<TableSnapshotBO> snapshotBOS = Optional.ofNullable(snapshots).orElse(List.of()).stream().map(TableMetaAppConvert.INSTANCE::toTableSnapshotBO).toList();
        tableMetaBO.setSnapshots(snapshotBOS);
    }
}
