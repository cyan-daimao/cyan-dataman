package com.cyan.dataman.infra.persistence.metadata.quality.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.dataman.domain.metadata.quality.MetadataQualityAlert;
import com.cyan.dataman.domain.metadata.quality.repository.MetadataQualityAlertRepository;
import com.cyan.dataman.infra.persistence.metadata.quality.convert.MetadataQualityInfraConvert;
import com.cyan.dataman.infra.persistence.metadata.quality.dos.MetadataQualityAlertDO;
import com.cyan.dataman.infra.persistence.metadata.quality.mappers.MetadataQualityAlertMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 元数据质量告警仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class MetadataQualityAlertRepositoryImpl implements MetadataQualityAlertRepository {

    private final MetadataQualityAlertMapper metadataQualityAlertMapper;

    public MetadataQualityAlertRepositoryImpl(MetadataQualityAlertMapper metadataQualityAlertMapper) {
        this.metadataQualityAlertMapper = metadataQualityAlertMapper;
    }

    /**
     * 保存告警
     */
    @Override
    public MetadataQualityAlert save(MetadataQualityAlert alert) {
        MetadataQualityAlertDO alertDO = MetadataQualityInfraConvert.INSTANCE.toAlertDO(alert);
        metadataQualityAlertMapper.insert(alertDO);
        return findById(alertDO.getId());
    }

    /**
     * 更新告警
     */
    @Override
    public MetadataQualityAlert update(MetadataQualityAlert alert) {
        MetadataQualityAlertDO alertDO = MetadataQualityInfraConvert.INSTANCE.toAlertDO(alert);
        metadataQualityAlertMapper.updateById(alertDO);
        return findById(alertDO.getId());
    }

    /**
     * 根据ID查询告警
     */
    @Override
    public MetadataQualityAlert findById(Long id) {
        return MetadataQualityInfraConvert.INSTANCE.toAlert(metadataQualityAlertMapper.selectById(id));
    }

    /**
     * 查询表告警
     */
    @Override
    public List<MetadataQualityAlert> listByTableId(String tableId) {
        LambdaQueryWrapper<MetadataQualityAlertDO> wrapper = new LambdaQueryWrapper<MetadataQualityAlertDO>()
                .eq(MetadataQualityAlertDO::getTableId, tableId)
                .orderByDesc(MetadataQualityAlertDO::getCreatedAt);
        List<MetadataQualityAlertDO> alertDOList = Optional.ofNullable(metadataQualityAlertMapper.selectList(wrapper)).orElse(List.of());
        return MetadataQualityInfraConvert.INSTANCE.toAlertList(alertDOList);
    }
}
