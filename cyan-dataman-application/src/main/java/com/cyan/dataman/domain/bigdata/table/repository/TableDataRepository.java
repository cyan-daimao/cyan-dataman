package com.cyan.dataman.domain.bigdata.table.repository;

import com.cyan.dataman.domain.bigdata.table.cmd.TableUploadCmd;
import com.cyan.dataman.domain.bigdata.table.query.TableDataListQuery;

import java.util.List;
import java.util.Map;

/**
 * 表-数据仓储
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface TableDataRepository {
    /**
     * 通过文件上传表数据
     */
    void upload(TableUploadCmd cmd);

    /**
     * 获得表数据
     */
    List<Map<String, Object>> list(TableDataListQuery query);

    /**
     * 获得表快照
     */
    void snapshots(TableDataListQuery query);
}
