package com.cyan.dataman.infra.bigdata.table.repository;

import com.cyan.arch.common.api.SilentException;
import com.cyan.arch.common.util.StrUtils;
import com.cyan.dataman.domain.bigdata.table.cmd.TableUploadCmd;
import com.cyan.dataman.domain.bigdata.table.query.TableDataListQuery;
import com.cyan.dataman.domain.bigdata.table.repository.TableDataRepository;
import com.cyan.dataman.enums.UploadFileType;
import com.cyan.dataman.infra.bigdata.table.handler.upload.TableDataUploadHandler;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * 表-数据服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class TableDataRepositoryImpl implements TableDataRepository {

    private final Map<UploadFileType, TableDataUploadHandler> uploadHandlerMap = new EnumMap<>(UploadFileType.class);
    private final SparkSession sparkSession;


    @Autowired
    public TableDataRepositoryImpl(List<TableDataUploadHandler> uploadHandlers, SparkSession sparkSession) {
        uploadHandlers.forEach(uploadHandler -> uploadHandlerMap.put(uploadHandler.getType(), uploadHandler));
        this.sparkSession = sparkSession;
    }


    /**
     * 通过文件上传表数据
     *
     * @param cmd 上传文件命令
     */
    @Override
    public void upload(TableUploadCmd cmd) {
        String originalFilename = cmd.getFile().getOriginalFilename();
        if (StrUtils.isBlank(originalFilename)) {
            throw new SilentException("上传数据：文件名不能为空");
        }
        TableDataUploadHandler tableDataUploadHandler = uploadHandlerMap.get(UploadFileType.getByCode(originalFilename.split("\\.")[1]));
        tableDataUploadHandler.upload(cmd);
    }

    /**
     * 获得表数据
     *
     * @param query 查询参数
     */
    @Override
    public List<Map<String, Object>> list(TableDataListQuery query) {
        List<Row> rows = sparkSession.table(query.fullName())
                .limit(query.getLimit())
                .collectAsList();
        return Optional.ofNullable(rows).orElse(List.of()).stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < row.size(); i++) {
                map.put(row.schema().fieldNames()[i], row.get(i));
            }
            return map;
        }).toList();
    }

    /**
     * 获得表快照
     */
    @Override
    public void snapshots(TableDataListQuery query) {
//        TableIdentifier tableIdentifier = TableIdentifier.of(query.getCatalog(),query.getDb(), query.getName());
//        Table table = icebergCatalog.loadTable(tableIdentifier);
//        table.snapshots().forEach(snapshot -> System.out.printf(
//                "快照 ID: %d, 创建时间: %s, 数据行数: %s%n",
//                snapshot.snapshotId(),
//                snapshot.timestampMillis(),  // 时间戳（可转成日期）
//                snapshot.summary().get("total-records")  // 该版本的数据行数
//        ));
    }
}
