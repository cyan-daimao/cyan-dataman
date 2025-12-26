package com.cyan.dataman.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分区类型
 *
 * @author cy.Y
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum PartitionType {

    /**
     * 原始字段
     * region = 华东 → 分区键 = 华东
     */
    IDENTITY("IDENTITY", "原始字段"),
    /**
     * 按天分区
     * create_time → 分区键 = 2025-11-10
     */
    DAY("DAY", "日期"),
    /**
     * 按小时分区
     * create_time → 分区键 = 2025-11-10-09
     */
    HOUR("HOUR", "小时"),
    /**
     * 按月分区
     * create_time → 分区键 = 2025-11
     */
    MONTH("MONTH", "月"),
    /**
     * 按年分区
     * create_time → 分区键 = 2025
     */
    YEAR("YEAR", "年"),
    /**
     * 按分桶分区
     * user_id=u123 → 哈希后分到 bucket=3（N=10）
     */
    BUCKET("BUCKET", "分桶"),
    /**
     * 按截断分区
     * phone=138xxxx1234 → 截断为 138（L=3）
     */
    TRUNCATE("TRUNCATE", "截断"),
    ;


    private final String code;

    private final String desc;

    public static PartitionType getByCode(String code) {
        for (PartitionType value : PartitionType.values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return null;
    }
}
