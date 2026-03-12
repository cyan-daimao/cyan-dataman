package com.cyan.dataman.application.metadata.cmd;

import com.cyan.dataman.domain.metadata.valobj.TableValObj;
import com.cyan.dataman.enums.DataLayer;
import com.cyan.dataman.enums.HeatLevel;
import com.cyan.dataman.enums.OnlineStatus;
import com.cyan.dataman.enums.SecretLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


/**
 * 元数据表命令
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class MetadataTableCmd {
    /**
     * 表名
     */
    @NotBlank(message = "表名不能为空")
    private String name;

    /**
     * 表负责人
     */
    @NotBlank(message = "表负责人不能为空")
    private String owner;

    /**
     * 主题编码
     */
    @NotBlank(message = "主题编码不能为空")
    private String subjectCode;

    /**
     * 数据层级
     */
    @NotNull(message = "数据层级")
    private DataLayer layerCode;

    /**
     * 表描述
     */
    @NotBlank(message = "表描述不能为空")
    private String comment;

    /**
     * 热度
     */
    @NotNull(message = "热度不能为空")
    private HeatLevel heatLevel;

    /**
     * 密级
     */
    @NotNull(message = "秘密等级不能为空")
    private SecretLevel secretLevel;

    /**
     * 在线
     */
    @NotNull(message = "在线状态")
    private OnlineStatus onlineStatus;

    /**
     * 表结构
     */
    @NotNull(message = "表结构不能为空")
    private TableValObj tableValObj;
}
