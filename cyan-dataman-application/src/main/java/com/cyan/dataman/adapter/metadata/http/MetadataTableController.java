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
import org.apache.iceberg.ExpireSnapshots;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.Table;
import org.apache.iceberg.actions.ActionsProvider;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.io.FileInfo;
import org.apache.iceberg.io.SupportsPrefixOperations;
import org.apache.iceberg.rest.RESTCatalog;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public MetadataTableController(MetadataTableService metadataTableService) {
        this.metadataTableService = metadataTableService;
    }

    /**
     * 获取表列表
     *
     * @param content     搜索内容可以是表名或表描述
     * @param subjectCode 主题编码
     * @param owner       负责人
     */
    @GetMapping
    public Response<Page<MetadataTableDTO>> page(@RequestParam(required = false) String content,
                                                 @RequestParam(required = false) String subjectCode,
                                                 @RequestParam(required = false) String owner,
                                                 @RequestParam(required = false) Long current,
                                                 @RequestParam(required = false) Long size) {
        current = current == null ? 1L : current;
        size = size == null ? 10L : size;
        MetadataTablePageQuery query = new MetadataTablePageQuery()
                .setOrName(content)
                .setOrComment(content)
                .setSubjectCode(subjectCode)
                .setOwner(owner);
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
     * 获取表快照
     */
    @GetMapping("/{fullName}/snapshot")
    public Response<Object> snapshot(@PathVariable String fullName) {
        RESTCatalog restCatalog = new RESTCatalog();
        restCatalog.initialize("iceberg", Map.of(
                "type", "rest",
                "uri", "http://iceberg-gravitino.cyan.com/iceberg/", // Gravitino Iceberg REST 地址
                "s3.endpoint", "http://rustfs.cyan.com",
                // S3 认证配置（必填，否则无法访问对象存储）
                "s3.access-key-id", "rustfsadmin",
                "s3.secret-access-key", "rustfsadmin",
                "s3.region", "cn-north-1"
        ));
        Table table = restCatalog.loadTable(TableIdentifier.of("ods", "ods_user_test"));

        Iterable<Snapshot> snapshots = table.snapshots();
        ArrayList<Snapshot> snapshotList = new ArrayList<>();
        for (Snapshot snapshot : snapshots) {
            snapshotList.add(snapshot);
        }
        snapshotList.sort((s1, s2) -> Long.compare(s2.timestampMillis(), s1.timestampMillis()));
        // 使快照过期
        ExpireSnapshots expireSnapshots = table.expireSnapshots();
        for (int i = 0; i < snapshotList.size(); i++) {
            Snapshot snapshot = snapshotList.get(i);
            if (i > 0) {
                expireSnapshots.expireSnapshotId(snapshot.snapshotId());
            }
        }
        expireSnapshots.cleanExpiredMetadata(true).cleanExpiredFiles(true).commit();
        table.refresh();
        FileIO fileIO = table.io();
        SupportsPrefixOperations prefixOperations = (SupportsPrefixOperations) fileIO;
        String location = table.location();
        Iterable<FileInfo> fileInfos = prefixOperations.listPrefix(location + "/metadata");
        for (FileInfo fileInfo : fileInfos) {
            //删除metadata.json文件
            String sequenceNumber = String.format("%05d", snapshotList.getFirst().sequenceNumber() + 1);
            if (fileInfo.location().endsWith(".metadata.json") && !fileInfo.location().startsWith(location + "/metadata/" + sequenceNumber)) {
                fileIO.deleteFile(fileInfo.location());
            }
            String avroUuid = fileInfo.location().replaceAll(location + "/metadata/", "").replaceAll("-m0.avro", "");
            if (fileInfo.location().endsWith(".avro") && !snapshotList.getFirst().manifestListLocation().contains(avroUuid)) {
                //删除.avro文件
                fileIO.deleteFile(fileInfo.location());
            }
        }
        return Response.success();
    }

}
