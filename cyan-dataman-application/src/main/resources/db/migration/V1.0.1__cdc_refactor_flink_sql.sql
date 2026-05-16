-- ============================================================
-- CDC 模块重构：Flink SQL + ODS 层
-- 变更内容：cdc_flink_job 表 cdc_config_id → ds_name
-- ============================================================

-- 1. 新增 ds_name 字段（数据源名称，一数据源一 Flink 作业）
ALTER TABLE cdc_flink_job
    ADD COLUMN ds_name VARCHAR(64) NULL COMMENT '数据源名称' AFTER id;

-- 2. 将现有 cdc_config_id 关联的数据源名称迁移到 ds_name
-- 注意：如果 cdc_config 记录已被删除，对应的 ds_name 将为 NULL，需手动处理
UPDATE cdc_flink_job f
    LEFT JOIN cdc_config c ON f.cdc_config_id = c.id
SET f.ds_name = c.ds_name
WHERE f.cdc_config_id IS NOT NULL;

-- 3. 为 ds_name 添加索引（按数据源查询作业）
CREATE INDEX idx_ds_name ON cdc_flink_job(ds_name);

-- 4. 删除旧的 cdc_config_id 字段及索引
ALTER TABLE cdc_flink_job DROP COLUMN cdc_config_id;

-- 5. 更新表注释
ALTER TABLE cdc_flink_job COMMENT = 'CDC Flink 作业配置表（一数据源一作业）';
