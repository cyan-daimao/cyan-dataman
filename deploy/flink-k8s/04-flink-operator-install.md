# Flink Kubernetes Operator 安装指南

## 前提条件

- K8s 集群 1.24+
- Helm 3.8+（推荐）
- kubectl 已配置好集群访问

## 1. 安装 Flink Kubernetes Operator

### 方式一：Helm（推荐）

```bash
# 添加 Flink Operator Helm 仓库
helm repo add flink-operator-repo https://downloads.apache.org/flink/flink-kubernetes-operator-1.9.0/
helm repo update

# 安装 Operator 到 flink namespace
helm install flink-kubernetes-operator flink-operator-repo/flink-kubernetes-operator \
  --namespace flink \
  --create-namespace \
  --set image.repository=flink-kubernetes-operator \
  --set image.tag=1.9.0

# 验证安装
kubectl get pods -n flink
# 应看到 flink-kubernetes-operator 的 Pod 在 Running 状态
```

### 方式二：kubectl apply（YAML 方式）

如果无法使用 Helm，可以下载官方 YAML 直接 apply：

```bash
# 下载官方 release YAML（包含 CRD + Deployment）
curl -L https://github.com/apache/flink-kubernetes-operator/releases/download/release-1.14.0/flink-kubernetes-operator-1.14.0.yaml \
  -o flink-operator.yaml

# 应用
kubectl apply -f flink-operator.yaml
```

> 注意：YAML 方式包含大量 CRD，apply 可能需要几十秒。

## 2. 验证 Operator 安装

```bash
# 查看 Operator Pod
kubectl get pods -n flink

# 查看 FlinkDeployment CRD 是否已注册
kubectl get crd | grep flinkdeployments

# 查看 Operator 日志
kubectl logs -n flink -l app.kubernetes.io/name=flink-kubernetes-operator
```

## 3. 配置 Spring Boot RBAC

Spring Boot 需要权限在 `flink` namespace 中创建 FlinkDeployment 和 ConfigMap。

```bash
# 应用 RBAC（假设 Spring Boot 在 cyan-dataman namespace 运行）
kubectl apply -f 03-rbac-for-springboot.yaml
```

如果你的 Spring Boot 使用非 `default` 的 ServiceAccount，修改 `03-rbac-for-springboot.yaml` 中的 `subjects` 部分：

```yaml
subjects:
  - kind: ServiceAccount
    name: 你的-serviceaccount-name
    namespace: 你的-namespace
```

## 4. 部署 Flink Application（CDC）

Spring Boot 启动后会自动调用 `CdcFlinkSyncServiceImpl.startFlinkSyncJob()`：

1. 查询所有 `enabled=true` + `syncTool=FLINK` 的 CDC 配置
2. 按 `dsName` 分组
3. 对每个数据源：
   - 生成 Flink SQL
   - 创建 ConfigMap（包含 SQL 脚本）
   - 创建 FlinkDeployment CR
4. Flink Operator 自动拉起 JM + TM Pod 执行 SQL

验证：

```bash
# 查看 FlinkDeployment 状态
kubectl get flinkdeployments -n flink

# 查看 Pod
kubectl get pods -n flink

# 查看某个 Flink 作业的日志
kubectl logs -n flink -l app=cdc-mysql-x99-cdc
```

## 5. 常见问题

| 问题 | 排查 |
|------|------|
| FlinkDeployment 状态为 `ERROR` | `kubectl describe flinkdeployment cdc-{dsName} -n flink` |
| Pod 无法启动 | 检查镜像是否已推送到 Harbor：`kubectl describe pod -n flink` |
| SQL 执行失败 | 查看 JM 日志：`kubectl logs -n flink deployment/{deployment-name}-jobmanager` |
| Spring Boot 报权限错误 | 确认 RBAC 已 apply，ServiceAccount 名称正确 |
