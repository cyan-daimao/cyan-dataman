# Flink Session Cluster K8s 部署指南

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                      K8s Namespace: cyan-dataman            │
│  ┌──────────────────┐      ┌──────────────────────────┐    │
│  │ Flink JobManager │──────│ Flink TaskManager (xN)   │    │
│  │  - REST API      │      │  - 执行 SQL Task         │    │
│  │  - Web UI :8081  │      │  - HPA 自动扩容          │    │
│  └──────────────────┘      └──────────────────────────┘    │
│         ↑                                                    │
│    Spring Boot App (同 Namespace)                            │
│    - 生成 Flink SQL                                          │
│    - POST /jars/:jarid/run 提交作业                          │
└─────────────────────────────────────────────────────────────┘
```

## 部署步骤

### 1. 构建自定义 Flink 镜像

基础镜像 `flink:2.0.1` 不包含 Kafka 和 Iceberg Connector，需要构建自定义镜像：

```bash
cd deploy/flink-k8s
docker build -t cyan/flink-sql:2.0.1 .

# 推送到镜像仓库（根据实际环境调整）
docker tag cyan/flink-sql:2.0.1 harbor.cyan.com/cyan/flink-sql:2.0.1
docker push harbor.cyan.com/cyan/flink-sql:2.0.1
```

> **注意**：`flink-sql-connector-kafka` 的版本 `4.0.1-2.0` 需要确认是否与 Flink 2.0.1 兼容。如果下载失败，请从 Maven 仓库查询正确的版本号。

### 2. 部署 Flink Session Cluster

```bash
# 创建 Namespace + ConfigMap + JobManager + TaskManager + HPA
kubectl apply -f 01-flink-session-cluster.yaml

# 或者使用自定义镜像版本
kubectl apply -f 02-flink-custom-image.yaml
```

### 3. 验证部署

```bash
# 查看 Pod 状态
kubectl get pods -n cyan-dataman

# 查看 JobManager 日志
kubectl logs -n cyan-dataman -l component=jobmanager

# 端口转发访问 Flink Web UI（本地调试）
kubectl port-forward -n cyan-dataman svc/flink-jobmanager 8081:8081
# 浏览器访问 http://localhost:8081
```

### 4. Spring Boot 连接配置

修改 `bootstrap-pre.yml` / `bootstrap-prod.yml`：

```yaml
flink:
  mode: remote
  rest:
    url: flink-jobmanager:8081  # K8s 内部 Service DNS
```

### 5. 提交 Flink SQL 作业

Spring Boot 启动后会自动调用 `CdcFlinkSyncServiceImpl.startFlinkSyncJob()`：

1. 查询所有 `enabled=true` + `syncTool=FLINK` 的 CDC 配置
2. 按 `dsName` 分组
3. 对每个数据源生成 Flink SQL
4. 通过 `StreamTableEnvironment.create(remoteEnv).executeSql()` 提交作业

## 扩容

TaskManager 已配置 HPA，当 CPU > 70% 或内存 > 80% 时自动扩容：

```bash
# 手动扩容
kubectl scale deployment flink-taskmanager -n cyan-dataman --replicas=5

# 查看当前副本数
kubectl get hpa -n cyan-dataman
```

## 清理

```bash
kubectl delete -f 01-flink-session-cluster.yaml
# 或
kubectl delete namespace cyan-dataman
```

## 故障排查

| 问题 | 排查方法 |
|------|----------|
| SQL 提交失败 | 查看 JobManager 日志：`kubectl logs -n cyan-dataman deployment/flink-jobmanager` |
| Kafka 连接失败 | 确认 TaskManager 能访问 Kafka Service DNS |
| Iceberg 写入失败 | 确认 S3/RustFS  endpoint 和凭据正确 |
| 类找不到 | 确认自定义镜像中包含 `iceberg-flink-runtime` 和 `flink-sql-connector-kafka` |
