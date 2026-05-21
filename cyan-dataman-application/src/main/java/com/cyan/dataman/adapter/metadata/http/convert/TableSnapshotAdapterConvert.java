package com.cyan.dataman.adapter.metadata.http.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataman.adapter.metadata.http.dto.TableSnapshotDTO;
import com.cyan.dataman.domain.metadata.valobj.TableSnapshotValObj;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 表快照适配器层转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface TableSnapshotAdapterConvert {

    TableSnapshotAdapterConvert INSTANCE = Mappers.getMapper(TableSnapshotAdapterConvert.class);

    TableSnapshotDTO toTableSnapshotDTO(TableSnapshotValObj valObj);

    List<TableSnapshotDTO> toTableSnapshotDTOList(List<TableSnapshotValObj> valObjs);
}
