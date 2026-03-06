package com.cyan.dataman.infra.persistence.metadata.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataman.infra.persistence.metadata.dos.MetadataSubjectDO;
import org.apache.ibatis.annotations.Mapper;

/**
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface MetadataSubjectMapper extends BaseMapper<MetadataSubjectDO> {
}
