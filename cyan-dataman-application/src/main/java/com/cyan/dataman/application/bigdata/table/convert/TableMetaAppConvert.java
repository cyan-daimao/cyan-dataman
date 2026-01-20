package com.cyan.dataman.application.bigdata.table.convert;

import com.cyan.dataman.application.bigdata.table.bo.TableMetaBO;
import com.cyan.dataman.application.bigdata.table.bo.TableSnapshotBO;
import com.cyan.dataman.domain.bigdata.table.TableMeta;
import com.cyan.dataman.domain.bigdata.table.TableSnapshot;
import com.cyan.dataman.domain.bigdata.table.cmd.TableMetaCmd;
import com.cyan.dataman.infra.util.GravitinoUtil;
import org.apache.gravitino.rel.Column;
import org.apache.gravitino.rel.Table;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.Map;

/**
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface TableMetaAppConvert {

    TableMetaAppConvert INSTANCE = Mappers.getMapper(TableMetaAppConvert.class);

    TableMetaBO toTableBO(TableMeta tableMeta);

    TableMeta toTable(TableMetaCmd tableMetaCmd);

    TableSnapshotBO toTableSnapshotBO(TableSnapshot tableSnapshot);

    default TableMeta toTableMeta(Table table, String catalog, String db) {
        String name = table.name();
        Column[] columns = table.columns();
        Map<String, String> properties = table.properties();
        return new TableMeta()
                .setCatalog(catalog)
                .setDb(db)
                .setTbl(name)
                .setFullName("%s.%s.%s".formatted(catalog, db, name))
                .setFields(GravitinoUtil.toFields(columns))
                .setComment(table.comment())
                .setLocation(properties.get("location"))
                ;
    }
}
