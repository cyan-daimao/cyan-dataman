package com.cyan.dataman.infra.util;

import com.cyan.dataman.domain.bigdata.table.Field;
import com.cyan.dataman.enums.FieldType;
import org.apache.gravitino.rel.Column;
import org.apache.gravitino.rel.types.Type;
import org.apache.gravitino.rel.types.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * gravitino 工具类
 *
 * @author cy.Y
 * @since 1.0.0
 */
public class GravitinoUtil {

    /**
     * 转换字段列表为 Column 数组
     *
     * @param fields 字段列表
     * @return Column 数组
     */
    public static Column[] toColumns(List<Field> fields) {

        return Optional.ofNullable(fields).orElse(List.of()).stream().map(field -> Column.of(field.getName(), toType(field), field.getComment())).toArray(Column[]::new);
    }

    public static Type toType(Field field) {
        FieldType type = field.getType();
        return switch (type) {
            case BOOLEAN -> Types.BooleanType.get();
            case INTEGER -> Types.IntegerType.get();
            case LONG -> Types.LongType.get();
            case FLOAT -> Types.FloatType.get();
            case DOUBLE -> Types.DoubleType.get();
            case DECIMAL -> Types.DecimalType.of(19, 6);
            case DATE -> Types.DateType.get();
            case TIME -> Types.TimeType.get();
            case TIMESTAMP -> Types.TimestampType.withTimeZone();
            case STRING -> Types.StringType.get();
            case UUID -> Types.UUIDType.get();
            case BINARY -> Types.BinaryType.get();
            case BYTE -> Types.ByteType.get();
            case VARIANT, FIXED, TIMESTAMP_NANO, STRUCT, LIST, MAP, UNKNOWN, GEOMETRY, GEOGRAPHY ->
                    Types.NullType.get();
        };
    }

    public static List<Field> toFields(Column[] columns) {
        List<Field> fields = new ArrayList<>();
        if (columns != null) {
            for (Column column : columns) {
                fields.add(toField(column));
            }
        }
        return fields;
    }

    public static Field toField(Column column) {
        return new Field()
                .setName(column.name())
                .setType(toFieldType(column.dataType()))
                .setComment(column.comment());
    }

    public static FieldType toFieldType(Type type) {
        return switch (type.name()) {
            case BOOLEAN -> FieldType.BOOLEAN;
            case INTEGER -> FieldType.INTEGER;
            case LONG -> FieldType.LONG;
            case FLOAT -> FieldType.FLOAT;
            case DOUBLE -> FieldType.DOUBLE;
            case DECIMAL -> FieldType.DECIMAL;
            case DATE -> FieldType.DATE;
            case TIME -> FieldType.TIME;
            case BINARY -> FieldType.BINARY;
            case BYTE -> FieldType.BYTE;
            case TIMESTAMP -> FieldType.TIMESTAMP;
            case STRING -> FieldType.STRING;
            case UUID -> FieldType.UUID;
            case SHORT, INTERVAL_YEAR, INTERVAL_DAY, VARCHAR, FIXEDCHAR, FIXED, STRUCT, LIST, MAP, UNION, NULL,
                 UNPARSED,
                 EXTERNAL -> FieldType.UNKNOWN;
        };
    }
}