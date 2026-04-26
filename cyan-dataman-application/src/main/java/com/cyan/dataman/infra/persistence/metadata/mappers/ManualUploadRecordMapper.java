package com.cyan.dataman.infra.persistence.metadata.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataman.infra.persistence.metadata.dos.ManualUploadRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 手动上传记录 Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface ManualUploadRecordMapper extends BaseMapper<ManualUploadRecordDO> {
}
