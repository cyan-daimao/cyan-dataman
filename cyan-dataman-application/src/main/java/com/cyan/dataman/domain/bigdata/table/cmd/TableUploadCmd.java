package com.cyan.dataman.domain.bigdata.table.cmd;

import com.cyan.dataman.enums.DataLayer;
import com.cyan.dataman.enums.WriteMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.web.multipart.MultipartFile;

/**
 * 表-上传命令
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class TableUploadCmd {

    /**
     * 上传文件
     */
    @NotNull(message = "上传的文件不能为空")
    private MultipartFile file;
    /**
     * 库名
     */
    @NotNull(message = "库名不能为空")
    private DataLayer db;

    /**
     * 表名
     */
    @NotBlank(message = "表名不能为空")
    private String name;

    /**
     * 写入模式
     */
    @NotNull(message = "写入模式不能为空")
    private WriteMode writeMode;


    /**
     * 库表名
     */
    public String getFullName() {
        return db.getCode() + "." + name;
    }
}
