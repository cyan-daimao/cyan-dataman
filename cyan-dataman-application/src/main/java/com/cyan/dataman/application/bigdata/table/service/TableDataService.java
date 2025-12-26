package com.cyan.dataman.application.bigdata.table.service;

import com.cyan.dataman.domain.bigdata.table.cmd.TableUploadCmd;
import com.cyan.dataman.domain.bigdata.table.query.TableDataListQuery;

import java.util.List;
import java.util.Map;

/**
 * 表-数据服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface TableDataService {

    /**
     * 上传表
     */
    void upload(TableUploadCmd cmd);

    /**
     * 获得表数据
     */
    List<Map<String, Object>> list(TableDataListQuery query);

}
