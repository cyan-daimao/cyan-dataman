CREATE TABLE IF NOT EXISTS metadata_lineage_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    node_key VARCHAR(512) NOT NULL COMMENT '节点唯一键',
    node_type VARCHAR(32) NOT NULL COMMENT '节点类型: FIELD/TABLE/ETL_JOB/METRIC',
    node_name VARCHAR(256) NOT NULL COMMENT '节点名称',
    service_name VARCHAR(64) COMMENT '来源服务名',
    ref_id VARCHAR(128) COMMENT '来源业务ID',
    table_ref VARCHAR(384) COMMENT '表唯一引用: catalog.schema.table',
    column_name VARCHAR(128) COMMENT '字段名',
    properties_json TEXT COMMENT '扩展属性JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    UNIQUE KEY uk_node_key (node_key),
    INDEX idx_node_type (node_type),
    INDEX idx_table_column (table_ref, column_name),
    INDEX idx_service_ref (service_name, ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元数据血缘节点表';

CREATE TABLE IF NOT EXISTS metadata_lineage_edge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    source_key VARCHAR(512) NOT NULL COMMENT '源节点唯一键',
    target_key VARCHAR(512) NOT NULL COMMENT '目标节点唯一键',
    edge_type VARCHAR(32) NOT NULL COMMENT '边类型: READS_FIELD/WRITES_FIELD/USES_FIELD/DERIVES_METRIC',
    service_name VARCHAR(64) COMMENT '来源服务名',
    ref_id VARCHAR(128) COMMENT '来源业务ID',
    properties_json TEXT COMMENT '扩展属性JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    INDEX idx_source_key (source_key),
    INDEX idx_target_key (target_key),
    INDEX idx_edge_type (edge_type),
    INDEX idx_service_ref (service_name, ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元数据血缘边表';
