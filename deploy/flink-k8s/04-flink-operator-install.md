# Flink Kubernetes Operator 安装指南

## 前提条件

- K8s 集群 1.24+
- Helm 3.8+（推荐）
- kubectl 已配置好集群访问

## 1. 安装 Flink Kubernetes Operator

> **Operator 镜像 vs Flink 作业镜像**
>
> - **Operator 镜像**（`apache/flink-kubernetes-operator:1.14.0`）：控制平面，管理 FlinkDeployment 生命周期。
>   用官方 Helm 安装即可，**不需要**用我们的 Dockerfile。
> - **Flink 作业镜像**（`harbor.cyan.com/cyan/flink-sql:2.0.1`）：数据平面，实际运行 SQL 的 JM + TM Pod。
>   由我们的 `Dockerfile` 构建，在 `FlinkDeployment.spec.image` 中指定。

### 方式一：Helm（推荐）

```bash
# 添加 Flink Operator Helm 仓库
helm repo add flink-operator-repo https://downloads.apache.org/flink/flink-kubernetes-operator-1.14.0/
helm repo update

# 安装 Operator 到 flink namespace（使用官方 Operator 镜像）
# 注意：默认启用 webhook 需要 cert-manager，这里先禁用 webhook 快速验证
helm install flink-kubernetes-operator flink-operator-repo/flink-kubernetes-operator \
  --namespace flink \
  --create-namespace \
  --set image.repository=apache/flink-kubernetes-operator \
  --set image.tag=1.14.0 \
  --set webhook.create=false

# 验证安装
kubectl get pods -n flink
# 应看到 flink-kubernetes-operator 的 Pod 在 Running 状态
```

> **注意**：`image.repository` 必须是 `apache/flink-kubernetes-operator`（带 `apache/` 前缀）。
> 如果写成 `flink-kubernetes-operator`，K8s 会默认当成 `docker.io/library/flink-kubernetes-operator`，导致 ImagePullBackOff。

### 如果无法访问 Docker Hub

你们的 K8s 集群如果拉不到 Docker Hub 镜像，可以先把 Operator 镜像推送到 Harbor：

```bash
# 在有外网的机器上
docker pull apache/flink-kubernetes-operator:1.14.0
docker tag apache/flink-kubernetes-operator:1.14.0 harbor.cyan.com/library/flink-kubernetes-operator:1.14.0
docker push harbor.cyan.com/library/flink-kubernetes-operator:1.14.0

# 然后 Helm 安装时改用 Harbor 地址
helm install flink-kubernetes-operator flink-operator-repo/flink-kubernetes-operator \
  --namespace flink \
  --create-namespace \
  --set image.repository=harbor.cyan.com/library/flink-kubernetes-operator \
  --set image.tag=1.14.0 \
  --set webhook.create=false
```

### 方式二：启用 webhook（生产环境推荐）

如果需要 webhook 功能（资源校验、默认值注入等），先安装 cert-manager：

```bash
# 1. 安装 cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.0/cert-manager.yaml

# 2. 等待 cert-manager 就绪
kubectl wait --for=condition=Ready pod -l app.kubernetes.io/instance=cert-manager -n cert-manager --timeout=120s

# 3. 安装 Flink Operator（不禁用 webhook）
helm install flink-kubernetes-operator flink-operator-repo/flink-kubernetes-operator \
  --namespace flink \
  --create-namespace \
  --set image.repository=apache/flink-kubernetes-operator \
  --set image.tag=1.14.0
```

### 方式三：kubectl apply（YAML 方式）

如果无法使用 Helm，可以下载官方 YAML 直接 apply：

```bash
# 下载官方 release YAML（包含 CRD + Deployment）
curl -L https://github.com/apache/flink-kubernetes-operator/releases/download/release-1.14.0/flink-kubernetes-operator-1.14.0.yaml \
  -o flink-operator.yaml

# 应用（同样需要先安装 cert-manager，或者手动编辑 YAML 禁用 webhook）
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

| 问题 | 原因 | 解决 |
|------|------|------|
| `ImagePullBackOff` / `pull access denied` | 镜像地址缺少 `apache/` 前缀 | `--set image.repository=apache/flink-kubernetes-operator` |
| `no matches for kind "Certificate"` | 未安装 cert-manager 且未禁用 webhook | 加 `--set webhook.create=false` 或先装 cert-manager |
| `secret "webhook-server-cert" not found` | webhook 启用但证书未生成 | 同上 |
| `FailedMount` / `keystore` | webhook 证书挂载失败 | 同上 |
| Pod 无法启动 | 镜像仓库无法访问 | 推送到 Harbor，改用内部地址 |
| SQL 执行失败 | Flink 作业镜像问题 | 查看 JM 日志：`kubectl logs -n flink deployment/{name}-jobmanager` |
| Spring Boot 权限报错 | RBAC 未配置 | 确认 `03-rbac-for-springboot.yaml` 已 apply |
