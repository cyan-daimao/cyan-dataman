package com.cyan.dataman.adapter.metadata.http;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.metadata.http.convert.TableAdapterConvert;
import com.cyan.dataman.adapter.metadata.http.dto.CatalogDTO;
import com.cyan.dataman.domain.metadata.valobj.SchemaValObj;
import com.cyan.dataman.adapter.metadata.http.dto.TableDTO;
import com.cyan.dataman.enums.DatasourceType;
import com.cyan.dataman.infra.util.StarRocksUtil;
import org.apache.gravitino.Catalog;
import org.apache.gravitino.NameIdentifier;
import org.apache.gravitino.Namespace;
import org.apache.gravitino.SupportsSchemas;
import org.apache.gravitino.client.GravitinoClient;
import org.apache.gravitino.rel.Table;
import org.apache.gravitino.rel.TableCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 目录接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/gravitino")
public class GravitinoController {
    private final GravitinoClient gravitinoClient;

    public GravitinoController(GravitinoClient gravitinoClient) {
        this.gravitinoClient = gravitinoClient;

    }

    /**
     * 获取目录列表
     */
    @GetMapping("/catalogs")
    public Response<List<CatalogDTO>> list() {
        Catalog[] catalogs = gravitinoClient.listCatalogsInfo();
        List<CatalogDTO> list = Arrays.stream(Optional.ofNullable(catalogs).orElse(new Catalog[0])).map(catalog -> new CatalogDTO().setName(catalog.name()).setDatasourceType(DatasourceType.getByCode(catalog.provider()))).toList();
        return Response.success(list);
    }

    /**
     * 获取库列表
     */
    @GetMapping("/catalog/{catalog}/schemas")
    public Response<List<SchemaValObj>> list(@PathVariable String catalog) {
        SupportsSchemas schemas = gravitinoClient.loadCatalog(catalog).asSchemas();
        List<SchemaValObj> list = Arrays.stream(Optional.ofNullable(schemas.listSchemas()).orElse(new String[0])).map(schema -> new SchemaValObj().setName(schema)).toList();
        return Response.success(list);
    }

    /**
     * 获取表列表
     */
    @GetMapping("/catalog/{catalog}/schema/{schema}/tables")
    public Response<List<TableDTO>> listTable(@PathVariable String catalog, @PathVariable String schema) {
        TableCatalog tableCatalog = gravitinoClient.loadCatalog(catalog).asTableCatalog();
        NameIdentifier[] nameIdentifiers = tableCatalog.listTables(Namespace.of(schema));
        List<TableDTO> list = Arrays.stream(Optional.ofNullable(nameIdentifiers).orElse(new NameIdentifier[0])).map(nameIdentifier ->
                new TableDTO()
                        .setCatalog(catalog)
                        .setSchema(nameIdentifier.namespace().toString())
                        .setName(nameIdentifier.name())
        ).toList();
        return Response.success(list);
    }

    /**
     * 获取表信息
     */
    @GetMapping("/catalog/{catalog}/schema/{schema}/table/{table}")
    public Response<TableDTO> listTableFields(@PathVariable String catalog, @PathVariable String schema, @PathVariable String table) {
        Table tableInfo = gravitinoClient.loadCatalog(catalog).asTableCatalog().loadTable(NameIdentifier.of(schema, table));
        TableDTO tableDTO = TableAdapterConvert.INSTANCE.tableToTableDTO(tableInfo);
        tableDTO.setCatalog(catalog);
        tableDTO.setSchema(schema);
        return Response.success(tableDTO);
    }


    /**
     * 获取表数据
     */
    @GetMapping("/catalog/{catalog}/schema/{schema}/table/{table}/data")
    public Response<List<Map<String, Object>>> listTableData(@PathVariable String catalog, @PathVariable String schema, @PathVariable String table) throws SQLException {
        String sql = "select * from %s.%s.%s limit 100".formatted(catalog, schema, table);
        List<Map<String, Object>> data = StarRocksUtil.queryForList(sql);
        return Response.success(data);
    }
}
