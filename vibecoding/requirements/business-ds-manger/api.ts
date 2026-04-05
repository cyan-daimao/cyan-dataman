/**
 * 数据源管理模块 - TypeScript 接口定义
 * 
 * @author cy.Y
 * @since 1.0.0
 */

// ==================== 枚举定义 ====================

/**
 * 数据源类型
 */
export enum DatasourceType {
  MYSQL = 'MYSQL',
  POSTGRESQL = 'POSTGRESQL',
  ICEBERG = 'ICEBERG',
}

/**
 * 字段数据类型
 */
export enum ColumnDataType {
  BOOLEAN = 'BOOLEAN',
  INTEGER = 'INTEGER',
  LONG = 'LONG',
  FLOAT = 'FLOAT',
  DOUBLE = 'DOUBLE',
  DECIMAL = 'DECIMAL',
  STRING = 'STRING',
  DATE = 'DATE',
  TIMESTAMP = 'TIMESTAMP',
  TIMESTAMP_TZ = 'TIMESTAMP_TZ',
  TIME = 'TIME',
  BINARY = 'BINARY',
  UUID = 'UUID',
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

// ==================== 类型定义 ====================

/**
 * 统一响应结构
 */
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

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
 * 字段信息
 */
export interface Column {
  name: string;
  type: ColumnDataType;
  comment: string;
  nullable: boolean;
  autoIncrement?: boolean;
  defaultValue?: string;
  secretLevel?: SecretLevel;
  precision?: number;
  scale?: number;
}

/**
 * 索引信息
 */
export interface Index {
  name: string;
  indexType: string;
  fieldNames: string[];
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
export const dsApi = {
  /**
   * 创建数据源配置
   * POST /api/v1/ds
   */
  create: (data: DsConfigCmd): Promise<ApiResponse<DsConfig>> => {
    return fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }).then(res => res.json());
  },

  /**
   * 获取数据源配置列表
   * GET /api/v1/ds
   */
  list: (query?: DsConfigListQuery): Promise<ApiResponse<DsConfig[]>> => {
    const params = new URLSearchParams();
    if (query?.name) params.append('name', query.name);
    if (query?.datasourceType) params.append('datasourceType', query.datasourceType);
    const url = params.toString() ? `${BASE_URL}?${params.toString()}` : BASE_URL;
    return fetch(url).then(res => res.json());
  },

  /**
   * 获取数据源配置详情
   * GET /api/v1/ds/{ds}
   */
  findById: (dsId: string): Promise<ApiResponse<DsConfig>> => {
    return fetch(`${BASE_URL}/${dsId}`).then(res => res.json());
  },

  /**
   * 更新数据源配置
   * PUT /api/v1/ds/{ds}
   */
  update: (dsId: string, data: DsConfigCmd): Promise<ApiResponse<DsConfig>> => {
    return fetch(`${BASE_URL}/${dsId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }).then(res => res.json());
  },

  /**
   * 删除数据源配置
   * DELETE /api/v1/ds/{ds}
   */
  delete: (dsId: string): Promise<ApiResponse<void>> => {
    return fetch(`${BASE_URL}/${dsId}`, {
      method: 'DELETE',
    }).then(res => res.json());
  },

  /**
   * 测试数据源连接
   * POST /api/v1/ds/{ds}/test
   */
  testConnection: (dsId: string): Promise<ApiResponse<void>> => {
    return fetch(`${BASE_URL}/${dsId}/test`, {
      method: 'POST',
    }).then(res => res.json());
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
  list: (dsId: string): Promise<ApiResponse<Database[]>> => {
    return fetch(`${BASE_URL}/${dsId}/dbs`).then(res => res.json());
  },

  /**
   * 创建数据库
   * POST /api/v1/ds/{ds}/dbs
   */
  create: (dsId: string, data: DatabaseCreateCmd): Promise<ApiResponse<void>> => {
    return fetch(`${BASE_URL}/${dsId}/dbs`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }).then(res => res.json());
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
  list: (dsId: string, dbName: string): Promise<ApiResponse<string[]>> => {
    return fetch(`${BASE_URL}/${dsId}/dbs/${dbName}/tables`).then(res => res.json());
  },

  /**
   * 获取表结构详情
   * GET /api/v1/ds/{ds}/dbs/{db}/tables/{tbl}
   */
  getSchema: (dsId: string, dbName: string, tableName: string): Promise<ApiResponse<TableSchema>> => {
    return fetch(`${BASE_URL}/${dsId}/dbs/${dbName}/tables/${tableName}`).then(res => res.json());
  },

  /**
   * 创建表
   * POST /api/v1/ds/{ds}/dbs/{db}/tables
   */
  create: (dsId: string, dbName: string, data: TableSchemaCmd): Promise<ApiResponse<void>> => {
    return fetch(`${BASE_URL}/${dsId}/dbs/${dbName}/tables`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }).then(res => res.json());
  },

  /**
   * 更新表结构
   * PUT /api/v1/ds/{ds}/dbs/{db}/tables/{tbl}
   */
  update: (dsId: string, dbName: string, tableName: string, data: TableSchemaCmd): Promise<ApiResponse<void>> => {
    return fetch(`${BASE_URL}/${dsId}/dbs/${dbName}/tables/${tableName}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }).then(res => res.json());
  },

  /**
   * 删除表
   * DELETE /api/v1/ds/{ds}/dbs/{db}/tables/{tbl}
   */
  drop: (dsId: string, dbName: string, tableName: string): Promise<ApiResponse<void>> => {
    return fetch(`${BASE_URL}/${dsId}/dbs/${dbName}/tables/${tableName}`, {
      method: 'DELETE',
    }).then(res => res.json());
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
