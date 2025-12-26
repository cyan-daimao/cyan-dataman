package com.cyan.dataman.adapter.http.bigdata.table;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.application.bigdata.table.service.TableDataService;
import com.cyan.dataman.domain.bigdata.table.cmd.TableUploadCmd;
import com.cyan.dataman.domain.bigdata.table.query.TableDataListQuery;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 表数据接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/table/data")
public class TableDataController {

    private final TableDataService tableDataService;

    public TableDataController(TableDataService tableDataService) {
        this.tableDataService = tableDataService;
    }

    /**
     * 获取表数据
     */
    @GetMapping("/list")
    public Response<List<Map<String, Object>>> list(@Validated TableDataListQuery query) {
        List<Map<String, Object>> list = tableDataService.list(query);
        return Response.success(list);
    }


    /**
     * 通过文件上传表数据
     */
    @PostMapping("/upload")
    public Response<Void> upload(@Validated TableUploadCmd cmd) {
        tableDataService.upload(cmd);
        return Response.success();
    }
}
