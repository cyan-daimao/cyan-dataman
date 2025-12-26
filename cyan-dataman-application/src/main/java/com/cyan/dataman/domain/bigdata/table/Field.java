package com.cyan.dataman.domain.bigdata.table;

import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.enums.FieldType;
import com.cyan.dataman.enums.PartitionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.iceberg.PartitionSpec;
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
     * 非分区字段: null
     */
    private Partition pt;

    /**
     * 转换为iceberg partition
     */
    public void toIcebergPartitionSpecBuilder(PartitionSpec.Builder builder) {
        if (pt == null) {
            return;
        }
        switch (pt.type) {
            case IDENTITY -> builder.identity(name);
            case DAY -> {
                if (type == FieldType.DATE || type == FieldType.TIMESTAMP || type == FieldType.TIMESTAMP_NANO) {
                    builder.day(name);
                    return;
                }
                throw new SilentException("day类型分区只支持date,time,timestamp,timestamp_nano");
            }
            case HOUR -> {
                if (type == FieldType.DATE || type == FieldType.TIMESTAMP || type == FieldType.TIMESTAMP_NANO) {
                    builder.hour(name);
                    return;
                }
                throw new SilentException("hour类型分区只支持date,time,timestamp,timestamp_nano");
            }
            case MONTH -> {
                if (type == FieldType.DATE || type == FieldType.TIMESTAMP || type == FieldType.TIMESTAMP_NANO) {
                    builder.month(name);
                    return;
                }
                throw new SilentException("month类型分区只支持date,time,timestamp,timestamp_nano");
            }
            case YEAR -> {
                if (type == FieldType.DATE || type == FieldType.TIMESTAMP || type == FieldType.TIMESTAMP_NANO) {
                    builder.year(name);
                    return;
                }
                throw new SilentException("year类型分区只支持date,time,timestamp,timestamp_nano");
            }
            case BUCKET -> builder.bucket(name, (Integer) this.pt.parameter);
            case TRUNCATE -> {
                if (type == FieldType.BINARY || type == FieldType.STRING || type == FieldType.INTEGER || type == FieldType.LONG || type == FieldType.DECIMAL) {
                    builder.truncate(name, (Integer) this.pt.parameter);
                    return;
                }
                throw new SilentException("truncate类型分区只支持binary,string,integer,long,decimal");
            }
        }
    }

    /**
     * 分区字段
     */
    public record Partition(PartitionType type, Object parameter) {

    }

    /**
     * 转换为iceberg类型
     *
     * @return iceberg类型
     */
    public Type getTypes() {
        return switch (type) {
            case BOOLEAN -> Types.BooleanType.get();
            case INTEGER -> Types.IntegerType.get();
            case LONG -> Types.LongType.get();
            case FLOAT -> Types.FloatType.get();
            case DOUBLE -> Types.DoubleType.get();
            case DATE -> Types.DateType.get();
            case TIME -> Types.TimeType.get();
            case TIMESTAMP -> Types.TimestampType.withZone();
            case TIMESTAMP_NANO -> Types.TimestampNanoType.withZone();
            case STRING -> Types.StringType.get();
            case UUID -> Types.UUIDType.get();
            case FIXED -> Types.FixedType.ofLength(255);
            case BINARY -> Types.BinaryType.get();
            case DECIMAL -> Types.DecimalType.of(19, 6);
            case GEOMETRY -> Types.GeometryType.crs84();
            case GEOGRAPHY -> Types.GeographyType.crs84();
            case STRUCT, LIST, MAP, UNKNOWN -> Types.UnknownType.get();
            case VARIANT -> Types.VariantType.get();
        };
    }

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