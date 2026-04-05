package com.cyan.dataman.domain.ds;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.domain.ds.repository.DsConfigRepository;
import com.cyan.dataman.enums.DatasourceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 数据源配置
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DsConfig {

    /**
     * 主键
     */
    private String id;

    /**
     * 数据源名称
     */
    private String name;

    /**
     * 数据源类型
     */
    private DatasourceType datasourceType;

    /**
     * 连接URL
     */
    private String url;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 修改人
     */
    private String updateBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    private LocalDateTime deletedAt;

    /**
     * 验证
     */
    private void validate() {
        Assert.notBlank(this.name, new SilentException("数据源名称不能为空"));
        Assert.notNull(this.datasourceType, new SilentException("数据源类型不能为空"));
        Assert.notBlank(this.url, new SilentException("连接URL不能为空"));
        Assert.notBlank(this.username, new SilentException("用户名不能为空"));
    }

    /**
     * 保存
     */
    public DsConfig save(DsConfigRepository dsConfigRepository) {
        validate();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        return dsConfigRepository.save(this);
    }

    /**
     * 更新
     */
    public DsConfig update(DsConfigRepository dsConfigRepository) {
        validate();
        this.updatedAt = LocalDateTime.now();
        return dsConfigRepository.update(this);
    }

    /**
     * 删除
     */
    public void delete(DsConfigRepository dsConfigRepository) {
        dsConfigRepository.deleteById(this.id);
    }
}
