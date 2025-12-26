package com.cyan.dataman.infra.bigdata.table.handler.upload;

import com.cyan.dataman.domain.bigdata.table.cmd.TableUploadCmd;
import com.cyan.dataman.enums.UploadFileType;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * csv方法
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
@Slf4j
public class CSVUploadHandler implements TableDataUploadHandler {
    private final SparkSession sparkSession;

    public CSVUploadHandler(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    /**
     * 往表里上传数据
     *
     * @param cmd 上传文件命令
     */
    @Override
    public void upload(TableUploadCmd cmd) {
        Dataset<Row> table = sparkSession.table(cmd.getFullName());
        Path tempFile = null;
        try {
            // 1. 写入临时文件
            tempFile = Files.createTempFile("upload_", ".csv");
            Files.copy(cmd.getFile().getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            // 2. 用 Spark 读取该临时文件
            Dataset<Row> csvData = sparkSession.read()
                    .option("header", "true")          // 如果有 header
                    .schema(table.schema())
                    .csv(tempFile.toString());
            // 3. 插入数据到目标表
            switch (cmd.getWriteMode()) {
                case APPEND -> csvData.writeTo(cmd.getFullName()).append();
                case OVERWRITE -> csvData.writeTo(cmd.getFullName()).createOrReplace();
            }
        } catch (IOException | NoSuchTableException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (tempFile != null) {
                    Files.deleteIfExists(tempFile);
                }
            } catch (IOException e) {
                log.error("Failed to delete temporary file: {}", tempFile, e);
            }
        }
    }

    /**
     * 获取上传文件类型
     *
     * @return 上传文件类型
     */
    @Override
    public UploadFileType getType() {
        return UploadFileType.CSV;
    }
}
