package com.cyan.dataman.domain.metadata.lineage;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.domain.metadata.lineage.repository.MetadataLineageRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 元数据血缘边
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MetadataLineageEdge {

    /**
     * 主键
     */
    private String id;

    /**
     * 源节点唯一键
     */
    private String sourceKey;

    /**
     * 目标节点唯一键
     */
    private String targetKey;

    /**
     * 边类型
     */
    private String edgeType;

    /**
     * 来源服务名
     */
    private String serviceName;

    /**
     * 来源业务ID
     */
    private String refId;

    /**
     * 扩展属性 JSON
     */
    private String propertiesJson;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;

    /**
     * 保存血缘边
     */
    public MetadataLineageEdge save(MetadataLineageRepository repository) {
        validate();
        return repository.saveEdge(this);
    }

    /**
     * 校验血缘边
     */
    private void validate() {
        Assert.notBlank(this.sourceKey, new SilentException("血缘源节点不能为空"));
        Assert.notBlank(this.targetKey, new SilentException("血缘目标节点不能为空"));
        Assert.notBlank(this.edgeType, new SilentException("血缘边类型不能为空"));
    }
}
