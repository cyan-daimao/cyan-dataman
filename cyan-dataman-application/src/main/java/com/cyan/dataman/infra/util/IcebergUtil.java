package com.cyan.dataman.infra.util;

import org.apache.iceberg.Table;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.rest.RESTCatalog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * iceberg工具类
 * @author cy.Y
 * @since v1.0.0
 */
@Component
public class IcebergUtil {

    private final RESTCatalog restCatalog;

    public IcebergUtil(@Qualifier("restCatalog") RESTCatalog restCatalog) {
        this.restCatalog = restCatalog;
    }

    /**
     * 获取iceberg rest catalog
     */
    @Bean("restCatalog")
    public RESTCatalog restCatalog (){
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
        return restCatalog;
    }

    /**
     * 合并数据
     * @param schema 库
     * @param tbl 表
     */
    public void mergeData(String schema,String tbl){
        Table table = restCatalog.loadTable(TableIdentifier.of(schema, tbl));
    }
}
