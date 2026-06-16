CREATE TABLE IF NOT EXISTS metadata_partition (
    id BIGINT PRIMARY KEY,
    data_catalog VARCHAR(128) NOT NULL COMMENT '目录',
    data_schema VARCHAR(128) NOT NULL COMMENT '库',
    tbl VARCHAR(255) NOT NULL COMMENT '表名',
    col VARCHAR(255) NOT NULL COMMENT '分区字段',
    partition_type VARCHAR(32) NOT NULL COMMENT '分区类型',
    param INT NULL COMMENT '分区参数，BUCKET 表示桶数，TRUNCATE 表示截断长度',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '分区顺序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted_at DATETIME NULL COMMENT '删除时间',
    KEY idx_metadata_partition_table (data_catalog, data_schema, tbl, deleted_at),
    KEY idx_metadata_partition_col (data_catalog, data_schema, tbl, col, deleted_at)
) COMMENT='元数据表分区';
