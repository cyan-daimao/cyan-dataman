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
 * 元数据血缘节点
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MetadataLineageNode {

    /**
     * 主键
     */
    private String id;

    /**
     * 节点唯一键
     */
    private String nodeKey;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 来源服务名
     */
    private String serviceName;

    /**
     * 来源业务ID
     */
    private String refId;

    /**
     * 表唯一引用
     */
    private String tableRef;

    /**
     * 字段名
     */
    private String columnName;

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
     * 保存或更新节点
     */
    public MetadataLineageNode saveOrUpdate(MetadataLineageRepository repository) {
        validate();
        return repository.saveOrUpdateNode(this);
    }

    /**
     * 校验节点
     */
    private void validate() {
        Assert.notBlank(this.nodeKey, new SilentException("血缘节点唯一键不能为空"));
        Assert.notBlank(this.nodeType, new SilentException("血缘节点类型不能为空"));
        Assert.notBlank(this.nodeName, new SilentException("血缘节点名称不能为空"));
    }
}
