package com.cyan.dataman.application.bigdata.table.service.impl;

import com.cyan.dataman.application.bigdata.table.service.TableDataService;
import com.cyan.dataman.domain.bigdata.table.cmd.TableUploadCmd;
import com.cyan.dataman.domain.bigdata.table.query.TableDataListQuery;
import com.cyan.dataman.domain.bigdata.table.repository.TableDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 表-数据服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class TableDataServiceImpl implements TableDataService {


    private final TableDataRepository tableDataRepository;

    @Autowired
    public TableDataServiceImpl(TableDataRepository tableDataRepository) {
        this.tableDataRepository = tableDataRepository;
    }

    /**
     * 上传表
     */
    @Override
    public void upload(TableUploadCmd cmd) {
        tableDataRepository.upload(cmd);
    }

    /**
     * 获得表数据
     */
    @Override
    public List<Map<String, Object>> list(TableDataListQuery query) {
        return tableDataRepository.list(query);
    }

}
