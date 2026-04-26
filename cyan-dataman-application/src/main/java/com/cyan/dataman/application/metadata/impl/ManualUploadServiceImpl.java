package com.cyan.dataman.application.metadata.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.application.metadata.ManualUploadService;
import com.cyan.dataman.domain.metadata.ManualUploadRecord;
import com.cyan.dataman.domain.metadata.MetadataTable;
import com.cyan.dataman.domain.metadata.repository.ManualUploadRecordRepository;
import com.cyan.dataman.domain.metadata.repository.MetadataTableRepository;
import com.opencsv.CSVReader;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 手动上传服务实现
 * <p>
 * 负责文件解析、Spark DataFrame 写入 Iceberg 表、保存上传记录
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class ManualUploadServiceImpl implements ManualUploadService {

    private final SparkSession spark;
    private final MetadataTableRepository metadataTableRepository;
    private final ManualUploadRecordRepository manualUploadRecordRepository;

    /**
     * 构造器注入
     */
    public ManualUploadServiceImpl(SparkSession spark,
                                    MetadataTableRepository metadataTableRepository,
                                    ManualUploadRecordRepository manualUploadRecordRepository) {
        this.spark = spark;
        this.metadataTableRepository = metadataTableRepository;
        this.manualUploadRecordRepository = manualUploadRecordRepository;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 1. 查询目标表信息（库名+表名）
     * 2. 解析 Excel/CSV 文件
     * 3. 通过 SparkSession 写入 Iceberg 表
     * 4. 保存上传记录（无论成功失败都会保存）
     */
    @Override
    @Transactional
    public ManualUploadRecord upload(Long tableId, MultipartFile file, String uploadMode, String uploader, String uploaderName) {
        ManualUploadRecord record = new ManualUploadRecord()
                .setTableId(tableId)
                .setFileName(file.getOriginalFilename())
                .setFileType(detectFileType(file.getOriginalFilename()))
                .setUploadMode(uploadMode)
                .setUploader(uploader)
                .setUploaderName(uploaderName)
                .setStatus("success");

        try {
            MetadataTable table = metadataTableRepository.findById(String.valueOf(tableId));
            Assert.notNull(table, new SilentException("表不存在"));
            String dbName = table.getTable().getSchema();
            String tableName = table.getTable().getName();
            String fullTableName = "rest." + dbName + "." + tableName;

            List<Map<String, Object>> dataList;
            if ("excel".equals(record.getFileType())) {
                dataList = parseExcel(file.getInputStream());
            } else {
                dataList = parseCsv(file.getInputStream());
            }
            record.setRowCount(dataList.size());

            writeToIceberg(fullTableName, dataList, uploadMode);
        } catch (Exception e) {
            record.setStatus("failed");
            record.setErrorMessage(e.getMessage());
        }

        return manualUploadRecordRepository.save(record);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<ManualUploadRecord> listRecords(Long tableId, long pageNum, long pageSize) {
        return manualUploadRecordRepository.pageByTableId(tableId, pageNum, pageSize);
    }

    /**
     * 根据文件名后缀检测文件类型
     *
     * @param fileName 文件名
     * @return excel 或 csv
     */
    private String detectFileType(String fileName) {
        if (fileName == null) return "csv";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            return "excel";
        }
        return "csv";
    }

    /**
     * 通过 SparkSession 将数据写入 Iceberg 表
     *
     * @param tableName  完整表名（如 rest.db.table）
     * @param dataList   数据列表
     * @param uploadMode 上传模式: overwrite/append
     * @throws Exception 写入异常
     */
    private void writeToIceberg(String tableName, List<Map<String, Object>> dataList, String uploadMode) throws Exception {
        if (dataList.isEmpty()) return;

        List<String> columns = new ArrayList<>(dataList.getFirst().keySet());

        List<Row> rows = new ArrayList<>();
        for (Map<String, Object> record : dataList) {
            Object[] values = columns.stream().map(record::get).toArray();
            rows.add(RowFactory.create(values));
        }

        StructField[] fields = columns.stream()
                .map(col -> DataTypes.createStructField(col, DataTypes.StringType, true))
                .toArray(StructField[]::new);
        StructType schema = DataTypes.createStructType(fields);

        Dataset<Row> df = spark.createDataFrame(rows, schema);

        if ("overwrite".equals(uploadMode)) {
            spark.sql("TRUNCATE TABLE " + tableName);
            df.writeTo(tableName).append();
        } else {
            df.writeTo(tableName).append();
        }
    }

    /**
     * 解析 Excel 文件
     *
     * @param inputStream 文件输入流
     * @return 数据列表（首行为表头）
     * @throws IOException IO异常
     */
    private List<Map<String, Object>> parseExcel(InputStream inputStream) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            org.apache.poi.ss.usermodel.Row headerRow = sheet.getRow(0);
            if (headerRow == null) return result;
            List<String> headers = new ArrayList<>();
            for (org.apache.poi.ss.usermodel.Cell cell : headerRow) {
                headers.add(cell.getStringCellValue().trim());
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                if (row == null) continue;
                Map<String, Object> map = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(j);
                    map.put(headers.get(j), getCellValue(cell));
                }
                result.add(map);
            }
        }
        return result;
    }

    /**
     * 获取单元格值（统一转为字符串）
     *
     * @param cell Excel单元格
     * @return 单元格字符串值
     */
    private Object getCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return cell.toString();
        }
    }

    /**
     * 解析 CSV 文件
     *
     * @param inputStream 文件输入流
     * @return 数据列表（首行为表头）
     * @throws Exception 解析异常
     */
    private List<Map<String, Object>> parseCsv(InputStream inputStream) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String[] headers = reader.readNext();
            if (headers == null) return result;
            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, Object> map = new LinkedHashMap<>();
                for (int i = 0; i < headers.length && i < line.length; i++) {
                    map.put(headers[i].trim(), line[i]);
                }
                result.add(map);
            }
        }
        return result;
    }
}
