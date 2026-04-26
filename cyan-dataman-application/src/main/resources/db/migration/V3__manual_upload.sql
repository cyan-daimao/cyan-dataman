create table cyan_dataman.manual_upload_record
(
    id            bigint auto_increment comment '主键'
        primary key,
    table_id      bigint                             not null comment '元数据表ID',
    file_name     varchar(256)                       not null comment '原始文件名',
    file_type     varchar(16)                        not null comment '文件类型: excel/csv',
    upload_mode   varchar(16)                        not null comment '上传模式: overwrite/append',
    row_count     int      default 0                 not null comment '导入行数',
    uploader      varchar(64)                        not null comment '上传人passport',
    uploader_name varchar(64)                        null comment '上传人姓名',
    status        varchar(16)                        not null comment '状态: success/failed',
    error_message text                               null comment '错误信息',
    created_at    datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted_at    datetime                           null comment '逻辑删除'
)
    comment '手动上传记录表' charset = utf8mb4;

create index idx_table_id
    on cyan_dataman.manual_upload_record (table_id);

