/**
 * 数据源管理模块 - TypeScript 接口定义
 *
 * @author cy.Y
 * @since 1.0.0
 */

// ==================== 枚举定义 ====================

import {ApiResponse} from "@/api/Response.ts";
import {datamanRequest} from "@/api/Request.ts";
import {AxiosRequestConfig} from "axios";

/**
 * 数据源类型
 */
export enum DatasourceType {
    MYSQL = 'MYSQL',
    POSTGRESQL = 'POSTGRESQL',
    ICEBERG = 'ICEBERG',
}

/**
 * 秘密等级
 */
export enum SecretLevel {
    L1 = 'L1',
    L2 = 'L2',
    L3 = 'L3',
    L4 = 'L4',
}

/**
 * MySQL 字段类型枚举
 */
export enum MysqlType {
    // 整数类型
    TINYINT = 'TINYINT',
    SMALLINT = 'SMALLINT',
    MEDIUMINT = 'MEDIUMINT',
    INT = 'INT',
    BIGINT = 'BIGINT',
    // 浮点类型
    FLOAT = 'FLOAT',
    DOUBLE = 'DOUBLE',
    DECIMAL = 'DECIMAL',
    // 字符串类型
    CHAR = 'CHAR',
    VARCHAR = 'VARCHAR',
    TEXT = 'TEXT',
    MEDIUMTEXT = 'MEDIUMTEXT',
    LONGTEXT = 'LONGTEXT',
    // 二进制类型
    BLOB = 'BLOB',
    MEDIUMBLOB = 'MEDIUMBLOB',
    LONGBLOB = 'LONGBLOB',
    // 日期时间类型
    DATE = 'DATE',
    TIME = 'TIME',
    DATETIME = 'DATETIME',
    TIMESTAMP = 'TIMESTAMP',
    YEAR = 'YEAR',
    // 其他类型
    BOOLEAN = 'BOOLEAN',
    JSON = 'JSON',
    ENUM = 'ENUM',
    SET = 'SET',
}

/**
 * PostgreSQL 字段类型枚举
 */
export enum PgsqlType {
    // 整数类型
    SMALLINT = 'SMALLINT',
    INTEGER = 'INTEGER',
    BIGINT = 'BIGINT',
    SMALLSERIAL = 'SMALLSERIAL',
    SERIAL = 'SERIAL',
    BIGSERIAL = 'BIGSERIAL',
    // 浮点类型
    REAL = 'REAL',
    DOUBLE_PRECISION = 'DOUBLE PRECISION',
    NUMERIC = 'NUMERIC',
    DECIMAL = 'DECIMAL',
    // 字符串类型
    CHAR = 'CHAR',
    VARCHAR = 'VARCHAR',
    TEXT = 'TEXT',
    // 二进制类型
    BYTEA = 'BYTEA',
    // 日期时间类型
    DATE = 'DATE',
    TIME = 'TIME',
    TIME_WITH_TIME_ZONE = 'TIME WITH TIME ZONE',
    TIMESTAMP = 'TIMESTAMP',
    TIMESTAMP_WITH_TIME_ZONE = 'TIMESTAMP WITH TIME ZONE',
    // 布尔类型
    BOOLEAN = 'BOOLEAN',
    // JSON 类型
    JSON = 'JSON',
    JSONB = 'JSONB',
    // UUID 类型
    UUID = 'UUID',
    // 数组类型
    ARRAY = 'ARRAY',
    // 网络地址类型
    CIDR = 'CIDR',
    INET = 'INET',
    MACADDR = 'MACADDR',
    // 几何类型
    POINT = 'POINT',
    LINE = 'LINE',
    LSEG = 'LSEG',
    BOX = 'BOX',
    PATH = 'PATH',
    POLYGON = 'POLYGON',
    CIRCLE = 'CIRCLE',
}

/**
 * 索引类型
 */
export enum IndexType {
    PRIMARY = 'PRIMARY',
    UNIQUE = 'UNIQUE',
    INDEX = 'INDEX',
    FULLTEXT = 'FULLTEXT',
}

/**
 * 索引方法
 */
export enum IndexMethod {
    BTREE = 'BTREE',
    HASH = 'HASH',
    GIN = 'GIN',
    GIST = 'GIST',
}

// ==================== 类型定义 ====================


/**
 * 数据源配置
 */
export interface DsConfig {
    id: string;
    name: string;
    datasourceType: DatasourceType;
    url: string;
    username: string;
    password: string;
    description: string;
    createBy: string;
    updateBy: string;
    createdAt: string;
    updatedAt: string;
}

/**
 * 数据源配置创建/更新参数
 */
export interface DsConfigCmd {
    name: string;
    datasourceType: DatasourceType;
    url: string;
    username: string;
    password?: string;
    description?: string;
}

/**
 * 数据源配置列表查询参数
 */
export interface DsConfigListQuery {
    name?: string;
    datasourceType?: DatasourceType;
}

/**
 * 数据库信息
 */
export interface Database {
    name: string;
    comment: string;
    charset: string;
    collation: string;
}

/**
 * 创建数据库参数
 */
export interface DatabaseCreateCmd {
    name: string;
    charset?: string;
    collation?: string;
}

/**
 * 字段信息基类
 */
export interface BaseColumn {
    /** 字段名称 */
    name: string;
    /** 字段类型（原始数据库类型字符串，如 VARCHAR(255), INT, BIGINT 等） */
    type: string;
    /** 字段注释 */
    comment: string;
    /** 字段是否为空 */
    nullable?: boolean;
    /** 字段是否自增 */
    autoIncrement?: boolean;
    /** 字段默认值 */
    defaultValue?: string;
    /** 字段密级 */
    secretLevel?: SecretLevel;
    /** 精度（如 DECIMAL(10,2) 中的 10，或 VARCHAR(255) 中的 255） */
    precision?: number;
    /** 小数位数（仅 DECIMAL 类型使用） */
    scale?: number;
}

/**
 * MySQL 字段信息
 */
export interface MysqlColumn extends BaseColumn {
    /** 无符号标识 */
    unsigned?: boolean;
    /** 零填充标识 */
    zerofill?: boolean;
    /** 字符集 */
    charset?: string;
    /** 排序规则 */
    collation?: string;
}

/**
 * PostgreSQL 字段信息
 */
export interface PgsqlColumn extends BaseColumn {
    /** 数组维度（PostgreSQL 支持数组类型） */
    arrayDimensions?: number;
    /** 时区标识 */
    withTimeZone?: boolean;
}

/**
 * 字段信息（兼容旧接口，根据数据源类型选择具体类型）
 */
export type Column = MysqlColumn | PgsqlColumn;

/**
 * 索引信息
 */
export interface Index {
    /** 索引名称 */
    name: string;
    /** 索引类型（PRIMARY, UNIQUE, INDEX, FULLTEXT） */
    indexType: string;
    /** 索引字段列表 */
    fieldNames: string[];
    /** 索引方法（BTREE, HASH, GIN, GIST） */
    indexMethod?: string;
    /** 索引注释 */
    comment?: string;
    /** 是否唯一索引 */
    unique?: boolean;
    /** 是否主键 */
    primaryKey?: boolean;
}

/**
 * 表结构信息
 */
export interface TableSchema {
    tableName: string;
    tableComment: string;
    columns: Column[];
    indexes: Index[];
}

/**
 * 创建/更新表结构参数
 */
export interface TableSchemaCmd {
    tableName: string;
    tableComment?: string;
    columns: Column[];
    indexes?: Index[];
}

// ==================== API 函数 ====================

const BASE_URL = '/api/v1/ds';

/**
 * 数据源管理 API
 */
export const DSApi = {
    /**
     * 创建数据源配置
     * POST /api/v1/ds
     */
    create: async (data: DsConfigCmd): Promise<ApiResponse<DsConfig>> => {
        return await datamanRequest.post(BASE_URL, data)
    },

    /**
     * 获取数据源配置列表
     * GET /api/v1/ds
     */
    list: async (query?: DsConfigListQuery): Promise<ApiResponse<DsConfig[]>> => {
        const config: AxiosRequestConfig = {
            params: query // GET 请求的参数要放在 params 里
        };
        return await datamanRequest.get(BASE_URL, config);
    },

    /**
     * 获取数据源配置详情
     * GET /api/v1/ds/{ds}
     */
    findById: async (dsId: string): Promise<ApiResponse<DsConfig>> => {
        return datamanRequest.get(`${BASE_URL}/${dsId}`);
    },

    /**
     * 更新数据源配置
     * PUT /api/v1/ds/{ds}
     */
    update: async (dsId: string, data: DsConfigCmd): Promise<ApiResponse<DsConfig>> => {
        return datamanRequest.put(`${BASE_URL}/${dsId}`, data);
    },

    /**
     * 删除数据源配置
     * DELETE /api/v1/ds/{ds}
     */
    delete: async (dsId: string): Promise<ApiResponse<void>> => {
        return datamanRequest.delete(`${BASE_URL}/${dsId}`)
    },

    /**
     * 测试数据源连接
     * POST /api/v1/ds/{ds}/test
     */
    testConnection: async (dsId: string): Promise<ApiResponse<void>> => {
        return datamanRequest.post(`${BASE_URL}/${dsId}/test`, {})
    },
};

/**
 * 数据库管理 API
 */
export const databaseApi = {
    /**
     * 获取数据源下的数据库列表
     * GET /api/v1/ds/{ds}/dbs
     */
    list: async (dsId: string): Promise<ApiResponse<Database[]>> => {
        return datamanRequest.get(`${BASE_URL}/${dsId}/dbs`);
    },

    /**
     * 创建数据库
     * POST /api/v1/ds/{ds}/dbs
     */
    create: async (dsId: string, data: DatabaseCreateCmd): Promise<ApiResponse<void>> => {
        return datamanRequest.post(`${BASE_URL}/${dsId}/dbs`,data)
    },
};

/**
 * 表管理 API
 */
export const tableApi = {
    /**
     * 获取数据库下的表列表
     * GET /api/v1/ds/{ds}/dbs/{db}/tables
     */
    list: async (dsId: string, dbName: string): Promise<ApiResponse<string[]>> => {
        return datamanRequest.get(`${BASE_URL}/${dsId}/dbs/${dbName}/tables`);
    },

    /**
     * 获取表结构详情
     * GET /api/v1/ds/{ds}/dbs/{db}/tables/{tbl}
     */
    getSchema: async (dsId: string, dbName: string, tableName: string): Promise<ApiResponse<TableSchema>> => {
        return datamanRequest.get(`${BASE_URL}/${dsId}/dbs/${dbName}/tables/${tableName}`);
    },

    /**
     * 创建表
     * POST /api/v1/ds/{ds}/dbs/{db}/tables
     */
    create: async (dsId: string, dbName: string, data: TableSchemaCmd): Promise<ApiResponse<void>> => {

        return datamanRequest.post(`${BASE_URL}/${dsId}/dbs/${dbName}/tables`,data)
    },

    /**
     * 更新表结构
     * PUT /api/v1/ds/{ds}/dbs/{db}/tables/{tbl}
     */
    update: async (dsId: string, dbName: string, tableName: string, data: TableSchemaCmd): Promise<ApiResponse<void>> => {
        return datamanRequest.put(`${BASE_URL}/${dsId}/dbs/${dbName}/tables/${tableName}`,data)
    },

    /**
     * 删除表
     * DELETE /api/v1/ds/{ds}/dbs/{db}/tables/{tbl}
     */
    drop: async (dsId: string, dbName: string, tableName: string): Promise<ApiResponse<void>> => {
        return datamanRequest.delete(`${BASE_URL}/${dsId}/dbs/${dbName}/tables/${tableName}`)
    },
};

// ==================== API 接口汇总表 ====================

/**
 * ## API 接口文档
 *
 * ### 数据源管理
 * | 方法 | 路径 | 说明 |
 * |------|------|------|
 * | POST | /api/v1/ds | 创建数据源配置 |
 * | GET | /api/v1/ds | 获取数据源配置列表 |
 * | GET | /api/v1/ds/{ds} | 获取数据源配置详情 |
 * | PUT | /api/v1/ds/{ds} | 更新数据源配置 |
 * | DELETE | /api/v1/ds/{ds} | 删除数据源配置 |
 * | POST | /api/v1/ds/{ds}/test | 测试数据源连接 |
 *
 * ### 数据库管理
 * | 方法 | 路径 | 说明 |
 * |------|------|------|
 * | GET | /api/v1/ds/{ds}/dbs | 获取数据库列表 |
 * | POST | /api/v1/ds/{ds}/dbs | 创建数据库 |
 *
 * ### 表管理
 * | 方法 | 路径 | 说明 |
 * |------|------|------|
 * | GET | /api/v1/ds/{ds}/dbs/{db}/tables | 获取表列表 |
 * | GET | /api/v1/ds/{ds}/dbs/{db}/tables/{tbl} | 获取表结构详情 |
 * | POST | /api/v1/ds/{ds}/dbs/{db}/tables | 创建表 |
 * | PUT | /api/v1/ds/{ds}/dbs/{db}/tables/{tbl} | 更新表结构 |
 * | DELETE | /api/v1/ds/{ds}/dbs/{db}/tables/{tbl} | 删除表 |
 *
 * ### 备注
 * - 创建表时会自动添加 `created_at`、`updated_at`、`deleted_at` 字段
 * - 所有接口返回统一的 `ApiResponse<T>` 结构
 */
