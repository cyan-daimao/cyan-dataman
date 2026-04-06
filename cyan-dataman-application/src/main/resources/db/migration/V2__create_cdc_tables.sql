-- CDC 配置管理模块数据库脚本

-- CDC 配置表
CREATE TABLE IF NOT EXISTS `cdc_config` (
    `id` VARCHAR(64) PRIMARY KEY COMMENT '主键',
    `name` VARCHAR(128) NOT NULL UNIQUE COMMENT 'CDC 配置名称（唯一标识）',
    `ds_id` VARCHAR(64) NOT NULL COMMENT '数据源 ID',
    `db_name` VARCHAR(128) NOT NULL COMMENT '数据库名',
    `table_name` VARCHAR(128) NOT NULL COMMENT '表名',
    `iceberg_table_name` VARCHAR(256) NOT NULL COMMENT '目标 Iceberg 表名',
    `sync_tool` VARCHAR(32) NOT NULL COMMENT '同步工具（SPARK/FLINK）',
    `sync_sql` TEXT COMMENT '同步 SQL',
    `enabled` TINYINT DEFAULT 0 COMMENT '是否启用（0-否，1-是）',
    `description` VARCHAR(512) COMMENT '描述',
    `create_by` VARCHAR(64) COMMENT '创建人',
    `update_by` VARCHAR(64) COMMENT '修改人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '逻辑删除时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDC 配置表';

-- CDC Spark 作业配置表
CREATE TABLE IF NOT EXISTS `cdc_spark_job` (
    `id` VARCHAR(64) PRIMARY KEY COMMENT '主键',
    `cdc_config_id` VARCHAR(64) NOT NULL COMMENT 'CDC 配置 ID',
    `sync_mode` VARCHAR(32) NOT NULL COMMENT '同步模式（OVERWRITE-覆盖，APPEND-追加）',
    `spark_sql` TEXT NOT NULL COMMENT 'Spark SQL 模板',
    `cron_expression` VARCHAR(64) COMMENT '调度表达式 (Cron)',
    `enabled` TINYINT DEFAULT 0 COMMENT '是否启用调度（0-否，1-是）',
    `create_by` VARCHAR(64) COMMENT '创建人',
    `update_by` VARCHAR(64) COMMENT '修改人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '逻辑删除时间',
    INDEX `idx_cdc_config_id` (`cdc_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDC Spark 作业配置表';

-- CDC Spark 任务实例表
CREATE TABLE IF NOT EXISTS `cdc_spark_task` (
    `id` VARCHAR(64) PRIMARY KEY COMMENT '主键',
    `spark_job_id` VARCHAR(64) NOT NULL COMMENT 'Spark 作业配置 ID',
    `cdc_config_id` VARCHAR(64) NOT NULL COMMENT 'CDC 配置 ID',
    `status` VARCHAR(32) NOT NULL COMMENT '任务状态（PENDING/RUNNING/SUCCESS/FAILED/STOPPED）',
    `application_id` VARCHAR(128) COMMENT 'Spark 应用 ID',
    `start_time` DATETIME COMMENT '开始时间',
    `end_time` DATETIME COMMENT '结束时间',
    `duration_seconds` BIGINT COMMENT '运行时长（秒）',
    `rows_affected` BIGINT COMMENT '影响行数',
    `error_message` TEXT COMMENT '错误信息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '逻辑删除时间',
    INDEX `idx_spark_job_id` (`spark_job_id`),
    INDEX `idx_cdc_config_id` (`cdc_config_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDC Spark 任务实例表';
