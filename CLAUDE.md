# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build
mvn clean package -DskipTests

# Run application
java -jar target/cyan-dataman.jar
```

## Architecture

**Multi-module Maven project** with DDD (Domain-Driven Design) architecture:

```
cyan-dataman/
├── cyan-dataman-application/  # Main application module
└── cyan-dataman-client/       # Client SDK with enums and shared types
```

### Layer Structure (within application module)

```
adapter/         # HTTP/RPC controllers, DTO conversion
application/     # Business logic orchestration, transactions
domain/          # Domain models, core business rules
infra/           # Database access, external integrations
```

### Key Constraints

- **Domain isolation**: `ds` domain valobjs (`domain/ds/valobj/`) and `metadata` domain valobjs (`domain/metadata/valobj/`) are independent - never cross-reference directly. Convert in adapter layer if needed.
- **Polyorphic valobjs**: `ColumnValObj` uses Jackson `@JsonTypeInfo` with `databaseType` discriminator for MySQL/PostgreSQL subclasses.

## Tech Stack

- Java 21, Spring Boot 3, MyBatis-Plus 3.5.7
- Apache Iceberg 1.10.1, Spark 4.0.2, Gravitino 1.1.0
- Nacos (service discovery & config)
- MapStruct (object mapping), Lombok

## Domains

### ds (Data Source)
Manages database connections (MySQL/PostgreSQL/Iceberg). Key entities: `DsConfig`, `TableSchemaValObj`.

### metadata
Metadata catalog with subject hierarchy (max 3 levels). Key entities: `MetadataSubject`, `MetadataTable`.

### cdc
CDC synchronization from source DB to Iceberg using Spark. Key entities: `CdcConfig`, `CdcSparkJob`, `CdcSparkTask`.

**CDC Config:** Defines source table (ds_id, db, table) and target Iceberg table.
**Spark Job:** Spark SQL template with sync mode (OVERWRITE/APPEND) and optional cron scheduling.
**Spark Task:** Task instance with status tracking (PENDING/RUNNING/SUCCESS/FAILED/STOPPED), duration, and row counts.

**APIs:**
```
POST   /api/v1/cdc                        # Create CDC config
GET    /api/v1/cdc                        # List CDC configs
GET    /api/v1/cdc/{cdcName}              # Get CDC config
PUT    /api/v1/cdc/{cdcName}              # Update CDC config
PUT    /api/v1/cdc/{cdcName}/open         # Toggle CDC (enabled/disabled)
DELETE /api/v1/cdc/{cdcName}              # Delete CDC config

POST   /api/v1/cdc/{cdcName}/spark-jobs   # Create Spark job config
GET    /api/v1/cdc/{cdcName}/spark-jobs   # List Spark job configs
PUT    /api/v1/cdc/spark-jobs/{jobId}     # Update Spark job config
DELETE /api/v1/cdc/spark-jobs/{jobId}     # Delete Spark job config

GET    /api/v1/cdc/{cdcName}/tasks        # List task instances
GET    /api/v1/cdc/tasks/{taskId}         # Get task instance details
POST   /api/v1/cdc/tasks/{taskId}/stop    # Stop running task
```

## API Conventions

- Base path: `/api/v1`
- Response format: `Response<T>` with `code`, `message`, `data`
- Logical delete via `deleted_at` field (MyBatis-Plus)
- Auto-fields on create: `created_at`, `updated_at`, `deleted_at`

## Naming Conventions

- Controllers: `XxxController`
- Services: `XxxService` / `XxxServiceImpl`
- Repositories: `XxxRepository` / `XxxRepositoryImpl`
- DTOs: `XxxDTO` (adapter), `XxxBO` (application), `XxxDO` (infra)
- Queries: `XxxQuery`, Commands: `XxxCmd`, Valobjs: `XxxValObj`

## Database Tables

- `ds_config` - Data source configuration
- `metadata_subject` - Metadata subject (hierarchical, max 3 levels)
- `metadata_table` - Metadata table
- `metadata_column` - Metadata column
- `metadata_index` - Metadata index
- `cdc_config` - CDC synchronization config
- `cdc_spark_job` - CDC Spark job configuration
- `cdc_spark_task` - CDC Spark task instances
