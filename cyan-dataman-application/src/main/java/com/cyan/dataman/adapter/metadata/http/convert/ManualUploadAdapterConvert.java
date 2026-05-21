package com.cyan.dataman.adapter.metadata.http.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.adapter.metadata.http.dto.ManualUploadRecordDTO;
import com.cyan.dataman.domain.metadata.ManualUploadRecord;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 手动上传记录适配器层转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface ManualUploadAdapterConvert {

    ManualUploadAdapterConvert INSTANCE = Mappers.getMapper(ManualUploadAdapterConvert.class);

    ManualUploadRecordDTO toManualUploadRecordDTO(ManualUploadRecord record);

    List<ManualUploadRecordDTO> toManualUploadRecordDTOList(List<ManualUploadRecord> records);
}
