package com.cyan.dataman.infra.persistence.metadata.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.domain.metadata.ManualUploadRecord;
import com.cyan.dataman.infra.persistence.metadata.dos.ManualUploadRecordDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 手动上传记录基础设施层转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface ManualUploadRecordInfraConvert {

    ManualUploadRecordInfraConvert INSTANCE = Mappers.getMapper(ManualUploadRecordInfraConvert.class);

    ManualUploadRecord toManualUploadRecord(ManualUploadRecordDO recordDO);

    ManualUploadRecordDO toManualUploadRecordDO(ManualUploadRecord record);

    List<ManualUploadRecord> toManualUploadRecordList(List<ManualUploadRecordDO> recordDOList);
}
