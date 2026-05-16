-- CDC 三期：添加主题编码字段，支持 ODS 统一表按主题前缀分表
ALTER TABLE cdc_config ADD COLUMN subject_code VARCHAR(64) AFTER table_name;
ALTER TABLE cdc_flink_job ADD COLUMN subject_code VARCHAR(64) AFTER ds_name;

-- 为历史数据设置默认值（如果存在 cdc 主题则设为 cdc，否则留空由业务补录）
-- UPDATE cdc_config SET subject_code = 'cdc' WHERE subject_code IS NULL;

CREATE INDEX idx_subject_code ON cdc_config(subject_code);
CREATE INDEX idx_flink_job_subject_code ON cdc_flink_job(subject_code);
