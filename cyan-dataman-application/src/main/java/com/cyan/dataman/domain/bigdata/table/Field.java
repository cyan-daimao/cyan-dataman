package com.cyan.dataman.domain.bigdata.table;

import com.cyan.dataman.enums.FieldType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;

/**
 * 字段
 *
 * @author cy.Y
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Field {

    /**
     * 字段名
     */
    private String name;

    /**
     * 字段类型
     */
    private FieldType type;

    /**
     * 字段是否可空
     */
    private Boolean required;

    /**
     * 字段描述
     */
    private String comment;


    /**
     * 转换字段类型
     *
     * @param type 字段类型
     * @return 字段类型
     */
    public static FieldType toFieldType(Type type) {
        if (type == null) {
            return FieldType.UNKNOWN;
        }
        switch (type) {
            case Types.BooleanType ignored -> {
                return FieldType.BOOLEAN;
            }
            case Types.IntegerType ignored -> {
                return FieldType.INTEGER;
            }
            case Types.LongType ignored -> {
                return FieldType.LONG;
            }
            case Types.FloatType ignored -> {
                return FieldType.FLOAT;
            }
            case Types.DoubleType ignored -> {
                return FieldType.DOUBLE;
            }
            case Types.DateType ignored -> {
                return FieldType.DATE;
            }
            case Types.TimeType ignored -> {
                return FieldType.TIME;
            }
            case Types.TimestampType ignored -> {
                return FieldType.TIMESTAMP;
            }
            case Types.TimestampNanoType ignored -> {
                return FieldType.TIMESTAMP_NANO;
            }
            case Types.StringType ignored -> {
                return FieldType.STRING;
            }
            case Types.UUIDType ignored -> {
                return FieldType.UUID;
            }
            case Types.FixedType ignored -> {
                return FieldType.FIXED;
            }
            case Types.BinaryType ignored -> {
                return FieldType.BINARY;
            }
            case Types.DecimalType ignored -> {
                return FieldType.DECIMAL;
            }
            case Types.GeometryType ignored -> {
                return FieldType.GEOMETRY;
            }
            case Types.GeographyType ignored -> {
                return FieldType.GEOGRAPHY;
            }
            case Types.StructType ignored -> {
                return FieldType.STRUCT;
            }
            case Types.ListType ignored -> {
                return FieldType.LIST;
            }
            case Types.MapType ignored -> {
                return FieldType.MAP;
            }
            case Types.VariantType ignored -> {
                return FieldType.VARIANT;
            }
            default -> {
                return FieldType.UNKNOWN;
            }
        }
    }
}