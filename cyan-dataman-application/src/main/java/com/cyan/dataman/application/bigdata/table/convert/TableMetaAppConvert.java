package com.cyan.dataman.application.bigdata.table.convert;

import com.cyan.dataman.application.bigdata.table.bo.TableMetaBO;
import com.cyan.dataman.application.bigdata.table.bo.TableSnapshotBO;
import com.cyan.dataman.domain.bigdata.table.TableMeta;
import com.cyan.dataman.domain.bigdata.table.TableSnapshot;
import com.cyan.dataman.domain.bigdata.table.cmd.TableMetaCmd;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author cy.Y
 */
@Mapper
public interface TableMetaAppConvert {

    TableMetaAppConvert INSTANCE = Mappers.getMapper(TableMetaAppConvert.class);

    TableMetaBO toTableBO(TableMeta tableMeta);

    TableMeta toTable(TableMetaCmd tableMetaCmd);

    TableSnapshotBO toTableSnapshotBO(TableSnapshot tableSnapshot);
}
