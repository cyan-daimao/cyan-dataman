package com.cyan.dataman.adapter.metadata.rpc;

import com.cyan.arch.common.api.Response;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.adapter.metadata.http.convert.MetadataTableAdapterConvert;
import com.cyan.dataman.adapter.metadata.http.dto.MetadataTableDTO;
import com.cyan.dataman.application.metadata.MetadataTableService;
import com.cyan.dataman.application.metadata.bo.MetadataColumnBO;
import com.cyan.dataman.application.metadata.bo.MetadataTableBO;
import com.cyan.dataman.application.metadata.cmd.MetadataTableCmd;
import com.cyan.dataman.client.table.request.MetadataColumnCreateRequest;
import com.cyan.dataman.client.table.request.MetadataTableCreateRequest;
import com.cyan.dataman.domain.metadata.query.MetadataTableOneQuery;
import com.cyan.dataman.domain.metadata.valobj.ColumnValObj;
import com.cyan.dataman.domain.metadata.valobj.TableValObj;
import com.cyan.dataman.enums.DataLayer;
import com.cyan.dataman.enums.OnlineStatus;
import com.cyan.dataman.enums.SecretLevel;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 元数据表 RPC 接口
 * <p>
 * 供其他微服务（如 cyan-data-collection）调用，不依赖登录态。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/rpc/v1/metadata/tables")
public class MetadataTableRPC {

    private final MetadataTableService metadataTableService;

    public MetadataTableRPC(MetadataTableService metadataTableService) {
        this.metadataTableService = metadataTableService;
    }

    /**
     * 创建表
     */
    @PostMapping
    public Response<MetadataTableDTO> save(@RequestBody MetadataTableCreateRequest request) {
        MetadataTableBO existing = metadataTableService.findOne(
                new com.cyan.dataman.domain.metadata.query.MetadataTableOneQuery().setName(request.getName()));
        if (existing != null) {
            return Response.success(MetadataTableAdapterConvert.INSTANCE.toMetadataTableDTO(existing));
        }
        MetadataTableCmd cmd = toCmd(request);
        MetadataTableBO bo = metadataTableService.save(cmd);
        MetadataTableDTO dto = MetadataTableAdapterConvert.INSTANCE.toMetadataTableDTO(bo);
        return Response.success(dto);
    }

    /**
     * 根据表名查询表
     */
    @GetMapping("/by-name/{name}")
    public Response<MetadataTableDTO> getByName(@PathVariable("name") String name) {
        MetadataTableBO bo = metadataTableService.findOne(new com.cyan.dataman.domain.metadata.query.MetadataTableOneQuery().setName(name));
        if (bo == null) {
            return Response.success(null);
        }
        MetadataTableDTO dto = MetadataTableAdapterConvert.INSTANCE.toMetadataTableDTO(bo);
        return Response.success(dto);
    }

    /**
     * 根据表标识查询字段列表
     */
    @GetMapping("/columns")
    public Response<List<com.cyan.dataman.client.table.dto.MetadataColumnDTO>> listColumns(
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema,
            @RequestParam String name) {
        MetadataTableBO table = metadataTableService.findOne(new MetadataTableOneQuery()
                .setCatalog(catalog)
                .setSchema(schema)
                .setName(name));
        if (table == null) {
            return Response.success(List.of());
        }
        List<MetadataColumnBO> columnBOs = metadataTableService.listColumns(table.getId());
        return Response.success(columnBOs.stream()
                .map(this::toClientColumnDTO)
                .toList());
    }

    private MetadataTableCmd toCmd(MetadataTableCreateRequest request) {
        DataLayer layer = DataLayer.getByCode(request.getLayerCode());
        if (layer == null) {
            throw new SilentException("无效的数据层级: " + request.getLayerCode());
        }
        SecretLevel secret = SecretLevel.getByCode(request.getSecretLevel());
        if (secret == null) {
            secret = SecretLevel.L1;
        }
        OnlineStatus online = OnlineStatus.getByCode(request.getOnlineStatus());
        if (online == null) {
            online = OnlineStatus.ONLINE;
        }

        List<ColumnValObj> columnValObjs = Optional.ofNullable(request.getColumns()).orElse(List.of())
                .stream()
                .map(this::toColumnValObj)
                .toList();

        TableValObj tableValObj = new TableValObj()
                .setCatalog("iceberg")
                .setSchema(layer.getCode().toLowerCase())
                .setName(request.getName())
                .setComment(request.getComment())
                .setColumns(columnValObjs);

        return new MetadataTableCmd()
                .setName(request.getName())
                .setOwner(request.getOwner())
                .setSubjectCode(request.getSubjectCode())
                .setLayerCode(layer)
                .setComment(request.getComment())
                .setSecretLevel(secret)
                .setOnlineStatus(online)
                .setTableValObj(tableValObj);
    }

    private ColumnValObj toColumnValObj(MetadataColumnCreateRequest col) {
        return new ColumnValObj()
                .setName(col.getName())
                .setType(col.getType())
                .setComment(col.getComment())
                .setNullable(col.getNullable() != null ? col.getNullable() : true);
    }

    private com.cyan.dataman.client.table.dto.MetadataColumnDTO toClientColumnDTO(MetadataColumnBO bo) {
        return new com.cyan.dataman.client.table.dto.MetadataColumnDTO()
                .setId(bo.getId())
                .setCol(bo.getCol())
                .setDataType(bo.getDataType())
                .setComment(bo.getComment())
                .setNullable(bo.getNullable())
                .setSecretLevel(bo.getSecretLevel())
                .setDefaultValue(bo.getDefaultValue())
                .setAutoIncrement(bo.getAutoIncrement());
    }
}
