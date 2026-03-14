package com.cyan.dataman.adapter.metadata.http.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.domain.metadata.valobj.ColumnValObj;
import com.cyan.dataman.domain.metadata.valobj.IndexValObj;
import com.cyan.dataman.adapter.metadata.http.dto.TableDTO;
import com.cyan.dataman.enums.ColumnDataType;
import org.apache.gravitino.rel.Column;
import org.apache.gravitino.rel.Table;
import org.apache.gravitino.rel.expressions.Expression;
import org.apache.gravitino.rel.expressions.FunctionExpression;
import org.apache.gravitino.rel.expressions.literals.Literals;
import org.apache.gravitino.rel.indexes.Index;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 表适配转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface TableAdapterConvert {
    TableAdapterConvert INSTANCE = Mappers.getMapper(TableAdapterConvert.class);

    default TableDTO tableToTableDTO(Table table) {
        //字段
        List<ColumnValObj> columns = Arrays.stream(Optional.ofNullable(table.columns()).orElse(new Column[0])).map(column -> {
                            String defaultValue = "";
                            Expression expression = column.defaultValue();
                            if (expression instanceof Literals.LiteralImpl literal) {
                                defaultValue = literal.value() == null ? "" : literal.value().toString();
                            }
                            if (expression instanceof FunctionExpression.FuncExpressionImpl funcExpression) {
                                defaultValue = funcExpression.functionName();
                            }
                            return new ColumnValObj()
                                    .setName(column.name())
                                    .setType(convertToColumnDataType(column.dataType().name()))
                                    .setDefaultValue(defaultValue)
                                    .setNullable(column.nullable())
                                    .setAutoIncrement(column.autoIncrement())
                                    .setComment(column.comment());
                        }

                )
                .toList();
        //索引
        Index[] index = table.index();
        List<IndexValObj> indexes = Arrays.stream(Optional.ofNullable(index).orElse(new Index[0])).map(idx ->
                        new IndexValObj()
                                .setName(idx.name())
                                .setIndexType(idx.type().name())
                                .setFieldNames(Arrays.stream(idx.fieldNames()).map(fieldName -> fieldName[0]).toList())
                )
                .toList();
        return new TableDTO()
                .setName(table.name())
                .setComment(table.comment())
                .setColumns(columns)
                .setIndexes(indexes);
    }

    /**
     * 将Gravitino类型名转换为ColumnDataType
     */
    private ColumnDataType convertToColumnDataType(org.apache.gravitino.rel.types.Type.Name typeName) {
        return switch (typeName) {
            case BOOLEAN -> ColumnDataType.BOOLEAN;
            case INTEGER -> ColumnDataType.INTEGER;
            case LONG -> ColumnDataType.LONG;
            case FLOAT -> ColumnDataType.FLOAT;
            case DOUBLE -> ColumnDataType.DOUBLE;
            case DECIMAL -> ColumnDataType.DECIMAL;
            case STRING -> ColumnDataType.STRING;
            case DATE -> ColumnDataType.DATE;
            case TIMESTAMP -> ColumnDataType.TIMESTAMP;
            case TIME -> ColumnDataType.TIME;
            case BINARY -> ColumnDataType.BINARY;
            case UUID -> ColumnDataType.UUID;
            default -> ColumnDataType.STRING;
        };
    }
}
