package com.cyan.dataman.adapter.metadata.http;

import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.metadata.http.convert.MetadataTableAdapterConvert;
import com.cyan.dataman.adapter.metadata.http.dto.MetadataTableDTO;
import com.cyan.dataman.adapter.metadata.http.dto.SubjectTableTreeDTO;
import com.cyan.dataman.application.metadata.MetadataTableService;
import com.cyan.dataman.application.metadata.bo.MetadataTableBO;
import com.cyan.dataman.application.metadata.cmd.MetadataTableCmd;
import com.cyan.dataman.domain.metadata.query.MetadataTablePageQuery;
import com.cyan.dataman.domain.metadata.valobj.TableSnapshotValObj;
import org.apache.iceberg.Table;
import org.apache.iceberg.spark.Spark3Util;
import org.apache.iceberg.spark.actions.SparkActions;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.apache.spark.sql.catalyst.parser.ParseException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * 元数据接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/metadata/tables")
public class MetadataTableController {
    private final MetadataTableService metadataTableService;
    private final SparkSession sparkSession;

    public MetadataTableController(MetadataTableService metadataTableService, SparkSession sparkSession) {
        this.metadataTableService = metadataTableService;
        this.sparkSession = sparkSession;
    }

    /**
     * 获取表列表
     *
     * @param content     搜索内容可以是表名或表描述
     * @param subjectCode 主题编码
     */
    @GetMapping
    public Response<Page<MetadataTableDTO>> page(@RequestParam(required = false) String content,
                                                 @RequestParam(required = false) String subjectCode,
                                                 @RequestParam(required = false) Long current,
                                                 @RequestParam(required = false) Long size) {
        current = current == null ? 1L : current;
        size = size == null ? 10L : size;
        MetadataTablePageQuery query = new MetadataTablePageQuery()
                .setContent(content)
                .setSubjectCode(subjectCode);
        query.setCurrent(current)
                .setSize(size);
        Page<MetadataTableBO> metadataTables = metadataTableService.page(query);
        List<MetadataTableDTO> data = Optional.ofNullable(metadataTables.getData()).orElse(List.of()).stream().map(MetadataTableAdapterConvert.INSTANCE::toMetadataTableDTO).toList();
        Page<MetadataTableDTO> page = new Page<>(data, metadataTables.getCurrent(), metadataTables.getSize(), metadataTables.getTotal());
        return Response.success(page);
    }


    /**
     * 获取表
     */
    @GetMapping("/{id}")
    public Response<MetadataTableDTO> findById(@PathVariable String id) {
        MetadataTableBO metadataTable = metadataTableService.findById(id);
        MetadataTableDTO metadataTableDTO = MetadataTableAdapterConvert.INSTANCE.toMetadataTableDTO(metadataTable);
        return Response.success(metadataTableDTO);
    }

    /**
     * 按表名获取表
     */
    @GetMapping("/by-name/{name}")
    public Response<MetadataTableDTO> getByName(@PathVariable("name") String name) {
        MetadataTableBO metadataTable = metadataTableService.findOne(new com.cyan.dataman.domain.metadata.query.MetadataTableOneQuery().setName(name));
        MetadataTableDTO metadataTableDTO = MetadataTableAdapterConvert.INSTANCE.toMetadataTableDTO(metadataTable);
        return Response.success(metadataTableDTO);
    }

    /**
     * 创建表
     */
    @PostMapping
    public Response<MetadataTableDTO> save(@RequestBody @Valid MetadataTableCmd cmd) {
        MetadataTableBO metadataTableBO = metadataTableService.save(cmd);
        MetadataTableDTO metadataTableDTO = MetadataTableAdapterConvert.INSTANCE.toMetadataTableDTO(metadataTableBO);
        return Response.success(metadataTableDTO);
    }

    /**
     * 更新表
     */
    @PutMapping("/{id}")
    public Response<MetadataTableDTO> update(@PathVariable String id, @RequestBody @Valid MetadataTableCmd cmd) {
        MetadataTableBO metadataTableBO = metadataTableService.update(id, cmd);
        MetadataTableDTO metadataTableDTO = MetadataTableAdapterConvert.INSTANCE.toMetadataTableDTO(metadataTableBO);
        return Response.success(metadataTableDTO);
    }

    /**
     * 删除表
     */
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable String id) {
        metadataTableService.delete(id);
        return Response.success();
    }

    /**
     * 获取主题-表树形结构
     *
     * @param content 搜索内容（表名或表描述），为空时返回全部树
     * @return 树形结构
     */
    @GetMapping("/tree")
    public Response<List<SubjectTableTreeDTO>> getSubjectTableTree(
            @RequestParam(required = false) String content) {
        List<SubjectTableTreeDTO> tree = metadataTableService.getSubjectTableTree(content);
        return Response.success(tree);
    }

    /**
     * 获取表字段列表
     */
    @GetMapping("/{id}/columns")
    public Response<List<com.cyan.dataman.adapter.metadata.http.dto.MetadataColumnDTO>> getTableColumns(@PathVariable String id) {
        List<com.cyan.dataman.application.metadata.bo.MetadataColumnBO> columnBOs = metadataTableService.listColumns(id);
        List<com.cyan.dataman.adapter.metadata.http.dto.MetadataColumnDTO> dtos = columnBOs.stream().map(bo -> {
            com.cyan.dataman.adapter.metadata.http.dto.MetadataColumnDTO dto = new com.cyan.dataman.adapter.metadata.http.dto.MetadataColumnDTO();
            dto.setId(bo.getId());
            dto.setCol(bo.getCol());
            dto.setDataType(bo.getDataType());
            dto.setComment(bo.getComment());
            dto.setNullable(bo.getNullable());
            dto.setSecretLevel(bo.getSecretLevel());
            dto.setDefaultValue(bo.getDefaultValue());
            dto.setAutoIncrement(bo.getAutoIncrement());
            return dto;
        }).toList();
        return Response.success(dtos);
    } // API: ready

    /**
     * 获取表快照
     */
    @GetMapping("/{fullName}/snapshots")
    public Response<List<TableSnapshotValObj>> snapshot(@PathVariable String fullName) {
        String[] split = fullName.split("\\.");
        String schema, tbl;
        if (split.length>2){
           schema = split[1];
           tbl = split[2];
        }else{
            schema = split[0];
            tbl = split[1];
        }
        List<TableSnapshotValObj> snapshots = metadataTableService.snapshots(schema, tbl);
        return Response.success(snapshots);
    }

    /**
     * 快照回滚
     */
    @GetMapping("/{fullName}/snapshots/{snapshotId}/rollback")
    public Response<Void> rollback(@PathVariable String fullName, @PathVariable String snapshotId) {
        String[] split = fullName.split("\\.");
        String schema, tbl;
        if (split.length>2){
            schema = split[1];
            tbl = split[2];
        }else{
            schema = split[0];
            tbl = split[1];
        }
        metadataTableService.rollback(schema, tbl, snapshotId);
        return Response.success();
    }

/**
     * 快照清理
     */
    @PostMapping("/{fullName}/maintenance")
    public Response<Void> maintenance(@PathVariable String fullName) throws NoSuchTableException, ParseException {
        String[] split = fullName.split("\\.");
        String schema, tbl;
        if (split.length>2){
            schema = split[1];
            tbl = split[2];
        }else{
            schema = split[0];
            tbl = split[1];
        }
        Table table = Spark3Util.loadIcebergTable(sparkSession, "%s.%s".formatted(schema,tbl));
        SparkActions actions = SparkActions.get(sparkSession);

        // 保留最近1秒的快照
        long olderThan = System.currentTimeMillis() - 1000;

        // 1. 过期快照（先删快照，再删文件，顺序不能乱）
        actions.expireSnapshots(table).expireOlderThan(olderThan).retainLast(10).execute();

        // 2. 合并小文件
        actions.rewriteDataFiles(table).option("target-file-size-bytes", Long.toString(128 * 1024 * 1024)).execute();

        // 3. 删除孤儿文件
        actions.deleteOrphanFiles(table).olderThan(olderThan).execute();

        return Response.success();
    }
}
