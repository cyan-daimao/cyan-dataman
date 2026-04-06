package com.cyan.dataman.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spark 任务状态
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum JobStatus {
    PENDING("PENDING", "待处理"),
    RUNNING("RUNNING", "运行中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    STOPPED("STOPPED", "已停止"),
    ;

    private final String code;
    private final String description;
}
