# Flink on K8s 部署指南（Application 模式）

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│  K8s                                                       │
│  ┌─ Namespace: flink ──────────────────────────────────┐   │
│  │  Flink Kubernetes Operator                          │   │
│  │  ├─ FlinkDeployment: cdc-mysql-x99-cdc  (JM+TM)    │   │
│  │  ├─ FlinkDeployment: cdc-pg-main-cdc    (JM+TM)    │   │
│  │  └─ FlinkDeployment: cdc-xxx-cdc        (JM+TM)    │   │
│  └─────────────────────────────────────────────────────┘   │
│         ↑                                                   │
│  ┌─ Spring Boot (任意 Namespace) ──────────────────────┐    │
│  │  - 生成 Flink SQL                                   │    │
│  │  - 创建 ConfigMap (SQL 脚本)                        │    │
│  │  - 创建 FlinkDeployment CR (K8s API)                │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## 部署步骤

### 1. 安装 Flink Kubernetes Operator

```bash
helm repo add flink-operator-repo https://downloads.apache.org/flink/flink-kubernetes-operator-1.9.0/
helm repo update

helm install flink-kubernetes-operator flink-operator-repo/flink-kubernetes-operator \
  --namespace flink --create-namespace
```

详细步骤见 [04-flink-operator-install.md](04-flink-operator-install.md)

### 2. 构建并推送自定义 Flink 镜像

```bash
# 1. 先打包 sql-runner.jar
mvn clean package -pl cyan-dataman-application -am -DskipTests

# 2. 构建镜像
cd deploy/flink-k8s
docker build -t harbor.cyan.com/cyan/flink-sql:2.0.1 .

# 3. 推送
docker push harbor.cyan.com/cyan/flink-sql:2.0.1
```

### 3. 配置 Spring Boot RBAC

Spring Boot 需要权限在 `flink` namespace 创建 FlinkDeployment 和 ConfigMap：

```bash
kubectl apply -f 03-rbac-for-springboot.yaml
```

> 如果你的 Spring Boot 不在 `cyan-dataman` namespace，修改 `03-rbac-for-springboot.yaml` 中的 `subjects`。

### 4. 部署 Flink Application（由 Spring Boot 自动创建）

Spring Boot 启动后自动调用 `CdcFlinkSyncServiceImpl.startFlinkSyncJob()`：

1. 查询所有 `enabled=true` + `syncTool=FLINK` 的 CDC 配置
2. 按 `dsName` 分组
3. 对每个数据源生成 Flink SQL
4. 创建 ConfigMap（SQL 脚本）
5. 创建 FlinkDeployment CR
6. Flink Operator 自动拉起 JM + TM Pod 执行 SQL

验证：

```bash
# 查看 FlinkDeployment
kubectl get flinkdeployments -n flink

# 查看 Pod
kubectl get pods -n flink

# 查看某个 CDC 作业的 JM 日志
kubectl logs -n flink deployment/cdc-mysql-x99-cdc-jobmanager
```

## 文件说明

| 文件 | 用途 |
|------|------|
| `Dockerfile` | 自定义 Flink 镜像（含 Kafka/Iceberg Connector + sql-runner.jar） |
| `01-flink-session-cluster.yaml` | Session Cluster 配置（已废弃，保留参考） |
| `02-flink-custom-image.yaml` | 使用自定义镜像的 Session Cluster（已废弃） |
| `03-rbac-for-springboot.yaml` | Spring Boot 操作 K8s API 所需的 RBAC |
| `04-flink-operator-install.md` | Operator 安装详细指南 |

## 故障排查

| 问题 | 排查方法 |
|------|----------|
| FlinkDeployment 状态 ERROR | `kubectl describe flinkdeployment cdc-{dsName} -n flink` |
| Pod ImagePullBackOff | 确认镜像已推送到 Harbor，`kubectl describe pod -n flink` |
| SQL 执行失败 | `kubectl logs -n flink deployment/{name}-jobmanager` |
| Spring Boot 权限报错 | 确认 `03-rbac-for-springboot.yaml` 已 apply |
| 类找不到 | 确认镜像中包含 `sql-runner.jar` 和 Connector JAR |
