package com.cyan.dataman.domain.ds.valobj;

import com.cyan.dataman.enums.MysqlColumnType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * MySQL 字段信息
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@JsonTypeName("MYSQL")
public class MysqlColumnValObj extends ColumnValObj {

    /**
     * MySQL 字段类型枚举
     */
    @Getter
    public enum MysqlType {
        // 整数类型
        TINYINT("TINYINT", "微整型(1字节)"),
        SMALLINT("SMALLINT", "小整型(2字节)"),
        MEDIUMINT("MEDIUMINT", "中整型(3字节)"),
        INT("INT", "整型(4字节)"),
        BIGINT("BIGINT", "大整型(8字节)"),

        // 浮点类型
        FLOAT("FLOAT", "单精度浮点"),
        DOUBLE("DOUBLE", "双精度浮点"),
        DECIMAL("DECIMAL", "高精度数值"),

        // 字符串类型
        CHAR("CHAR", "定长字符串"),
        VARCHAR("VARCHAR", "变长字符串"),
        TEXT("TEXT", "长文本"),
        MEDIUMTEXT("MEDIUMTEXT", "中等长度文本"),
        LONGTEXT("LONGTEXT", "长文本"),

        // 二进制类型
        BLOB("BLOB", "二进制大对象"),
        MEDIUMBLOB("MEDIUMBLOB", "中等长度二进制"),
        LONGBLOB("LONGBLOB", "长二进制"),

        // 日期时间类型
        DATE("DATE", "日期"),
        TIME("TIME", "时间"),
        DATETIME("DATETIME", "日期时间"),
        TIMESTAMP("TIMESTAMP", "时间戳"),
        YEAR("YEAR", "年份"),

        // 其他类型
        BOOLEAN("BOOLEAN", "布尔类型"),
        JSON("JSON", "JSON类型"),
        ENUM("ENUM", "枚举类型"),
        SET("SET", "集合类型"),
        ;

        private final String code;
        private final String desc;

        MysqlType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public static MysqlType getByCode(String code) {
            if (code == null) {
                return null;
            }
            String upperCode = code.toUpperCase();
            for (MysqlType value : values()) {
                if (value.code.equals(upperCode)) {
                    return value;
                }
            }
            // 处理带括号的类型，如 VARCHAR(255)
            for (MysqlType value : values()) {
                if (upperCode.startsWith(value.code)) {
                    return value;
                }
            }
            return null;
        }
    }

    /**
     * 无符号标识
     */
    private Boolean unsigned;

    /**
     * 零填充标识
     */
    private Boolean zerofill;

    /**
     * 字符集
     */
    private String charset;

    /**
     * 排序规则
     */
    private String collation;

    @Override
    public String getTypeEnumCode() {
        MysqlType mysqlType = getMysqlTypeEnum();
        return mysqlType != null ? mysqlType.getCode() : this.type;
    }

    /**
     * 获取 MySQL 字段类型枚举
     */
    public MysqlType getMysqlTypeEnum() {
        return MysqlType.getByCode(this.type);
    }

    /**
     * 使用 MysqlType 枚举设置字段类型
     */
    public MysqlColumnValObj setMysqlType(MysqlType mysqlType) {
        this.type = mysqlType.getCode();
        return this;
    }

    /**
     * 使用 client 模块的 MysqlColumnType 枚举设置字段类型
     */
    public MysqlColumnValObj setMysqlColumnType(MysqlColumnType mysqlColumnType) {
        this.type = mysqlColumnType.getCode();
        return this;
    }

    /**
     * 获取 client 模块的 MysqlColumnType 枚举
     */
    public MysqlColumnType getMysqlColumnType() {
        return MysqlColumnType.getByCode(this.type);
    }

    /**
     * 判断是否为整数类型
     */
    public boolean isIntegerType() {
        MysqlType mysqlType = getMysqlTypeEnum();
        if (mysqlType == null) {
            return false;
        }
        return mysqlType == MysqlType.TINYINT
                || mysqlType == MysqlType.SMALLINT
                || mysqlType == MysqlType.MEDIUMINT
                || mysqlType == MysqlType.INT
                || mysqlType == MysqlType.BIGINT;
    }

    /**
     * 判断是否为字符串类型
     */
    public boolean isStringType() {
        MysqlType mysqlType = getMysqlTypeEnum();
        if (mysqlType == null) {
            return false;
        }
        return mysqlType == MysqlType.CHAR
                || mysqlType == MysqlType.VARCHAR
                || mysqlType == MysqlType.TEXT
                || mysqlType == MysqlType.MEDIUMTEXT
                || mysqlType == MysqlType.LONGTEXT;
    }

    /**
     * 判断是否为时间类型
     */
    public boolean isDateTimeType() {
        MysqlType mysqlType = getMysqlTypeEnum();
        if (mysqlType == null) {
            return false;
        }
        return mysqlType == MysqlType.DATE
                || mysqlType == MysqlType.TIME
                || mysqlType == MysqlType.DATETIME
                || mysqlType == MysqlType.TIMESTAMP
                || mysqlType == MysqlType.YEAR;
    }
}
