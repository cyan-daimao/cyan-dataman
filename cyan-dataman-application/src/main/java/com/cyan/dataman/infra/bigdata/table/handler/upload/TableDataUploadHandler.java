package com.cyan.dataman.infra.bigdata.table.handler.upload;

import com.cyan.dataman.domain.bigdata.table.cmd.TableUploadCmd;
import com.cyan.dataman.enums.UploadFileType;

/**
 * 表-数据上传方法
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface TableDataUploadHandler {

    /**
     * 往表里上传数据
     *
     * @param cmd 上传文件命令
     */
    void upload(TableUploadCmd cmd);

    /**
     * 获取上传文件类型
     *
     * @return 上传文件类型
     */
    UploadFileType getType();
}
