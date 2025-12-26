package com.cyan.dataman.adapter.http.bigdata.table.convert;

import com.cyan.dataman.adapter.http.bigdata.table.dto.TableMetaDTO;
import com.cyan.dataman.adapter.http.bigdata.table.dto.TableSnapshotsDTO;
import com.cyan.dataman.application.bigdata.table.bo.TableMetaBO;
import com.cyan.dataman.application.bigdata.table.bo.TableSnapshotBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author cy.Y
 * @since 1.0.0
 */
//@Mapper(uses = MapstructConvert.class)
@Mapper
public interface TableMetaAdapterConvert {
    TableMetaAdapterConvert INSTANCE = Mappers.getMapper(TableMetaAdapterConvert.class);

    TableMetaDTO toTableDTO(TableMetaBO table);

    TableSnapshotsDTO toTableSnapshotsDTO(TableSnapshotBO tableSnapshotBO);
}
