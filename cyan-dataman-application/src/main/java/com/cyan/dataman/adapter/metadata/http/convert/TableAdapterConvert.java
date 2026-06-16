package com.cyan.dataman.adapter.metadata.http.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataman.domain.metadata.valobj.ColumnValObj;
import com.cyan.dataman.domain.metadata.valobj.IndexValObj;
import com.cyan.dataman.domain.metadata.valobj.PartitionValObj;
import com.cyan.dataman.adapter.metadata.http.dto.TableDTO;
import com.cyan.dataman.enums.PartitionType;
import org.apache.gravitino.rel.Column;
import org.apache.gravitino.rel.Table;
import org.apache.gravitino.rel.expressions.Expression;
import org.apache.gravitino.rel.expressions.NamedReference;
import org.apache.gravitino.rel.expressions.FunctionExpression;
import org.apache.gravitino.rel.expressions.literals.Literals;
import org.apache.gravitino.rel.expressions.transforms.Transform;
import org.apache.gravitino.rel.expressions.transforms.Transforms;
import org.apache.gravitino.rel.indexes.Index;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.Locale;
import java.util.List;
import java.util.Optional;

/**
 * 表适配转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
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
                                    .setType(convertToDbTypeString(column.dataType().name()))
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
        // 分区
        List<PartitionValObj> partitions = Arrays.stream(Optional.ofNullable(table.partitioning()).orElse(new Transform[0]))
                .map(this::transformToPartition)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        return new TableDTO()
                .setName(table.name())
                .setComment(table.comment())
                .setColumns(columns)
                .setIndexes(indexes)
                .setPartitions(partitions);
    }

    /**
     * 将Gravitino分区表达式转换为分区值对象
     */
    private Optional<PartitionValObj> transformToPartition(Transform transform) {
        PartitionType partitionType = PartitionType.getByCode(transform.name().toUpperCase(Locale.ROOT));
        if (partitionType == null) {
            return Optional.empty();
        }
        String columnName = firstReferenceName(transform);
        if (columnName == null || columnName.isBlank()) {
            return Optional.empty();
        }
        PartitionValObj partition = new PartitionValObj()
                .setColumnName(columnName)
                .setPartitionType(partitionType);
        if (transform instanceof Transforms.BucketTransform bucketTransform) {
            partition.setParam(bucketTransform.numBuckets());
        }
        if (transform instanceof Transforms.TruncateTransform truncateTransform) {
            partition.setParam(truncateTransform.width());
        }
        return Optional.of(partition);
    }

    /**
     * 获取分区表达式首个字段名
     */
    private String firstReferenceName(Transform transform) {
        NamedReference[] references = Optional.ofNullable(transform.references()).orElse(new NamedReference[0]);
        if (references.length == 0 || references[0].fieldName().length == 0) {
            return null;
        }
        String[] fieldName = references[0].fieldName();
        return fieldName[fieldName.length - 1];
    }

    /**
     * 将Gravitino类型名转换为数据库类型字符串
     */
    private String convertToDbTypeString(org.apache.gravitino.rel.types.Type.Name typeName) {
        return switch (typeName) {
            case BOOLEAN -> "BOOLEAN";
            case INTEGER -> "INTEGER";
            case LONG -> "BIGINT";
            case FLOAT -> "FLOAT";
            case DOUBLE -> "DOUBLE";
            case DECIMAL -> "DECIMAL";
            case STRING -> "VARCHAR(255)";
            case DATE -> "DATE";
            case TIMESTAMP -> "TIMESTAMP";
            case TIME -> "TIME";
            case BINARY -> "BLOB";
            case UUID -> "UUID";
            default -> "VARCHAR(255)";
        };
    }
}
