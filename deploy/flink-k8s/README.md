# Flink on K8s 部署指南（Application 模式）

## 架构

```
Spring Boot ──K8s API──→ Flink Kubernetes Operator ──→ FlinkDeployment CR
                                                          │
                                                          ↓
                                                    ┌─ JM + TM Pod ─┐
                                                    │  SqlRunner    │
                                                    │  执行 SQL     │
                                                    └───────────────┘
```

- **Operator**：官方镜像 `apache/flink-kubernetes-operator:1.14.0`，管理 FlinkDeployment 生命周期
- **Flink 作业镜像**：`harbor.cyan.com/cyan/flink-sql:2.0.1`，由 `Dockerfile` 构建（含 Kafka/Iceberg Connector + sql-runner.jar）

## 前置条件

- K8s 集群 1.24+
- Helm 3.8+
- kubectl 已配置集群访问
- Harbor 已配置且可推送镜像

---

## 部署步骤

### 1. 安装 Flink Kubernetes Operator

```bash
helm repo add flink-operator-repo https://downloads.apache.org/flink/flink-kubernetes-operator-1.14.0/
helm repo update

helm install flink-kubernetes-operator flink-operator-repo/flink-kubernetes-operator \
  --namespace flink \
  --create-namespace \
  --set image.repository=apache/flink-kubernetes-operator \
  --set image.tag=1.14.0 \
  --set webhook.create=false
```

> 如果 K8s 无法访问 Docker Hub，先把镜像推送到 Harbor：
> ```bash
> docker pull apache/flink-kubernetes-operator:1.14.0
> docker tag apache/flink-kubernetes-operator:1.14.0 harbor.cyan.com/library/flink-kubernetes-operator:1.14.0
> docker push harbor.cyan.com/library/flink-kubernetes-operator:1.14.0
> ```
> 然后 Helm 安装时把 `image.repository` 改成 `harbor.cyan.com/library/flink-kubernetes-operator`。

验证：
```bash
kubectl get pods -n flink
kubectl get crd | grep flinkdeployments
```

### 2. 构建并推送 Flink 作业镜像

```bash
# 先打包 sql-runner.jar
mvn clean package -pl cyan-dataman-flink-runner -am -DskipTests

# 构建镜像
cd deploy/flink-k8s
docker build -t harbor.cyan.com/cyan/flink-sql:2.0.1 .

# 推送
docker push harbor.cyan.com/cyan/flink-sql:2.0.1
```

### 3. apply RBAC

Spring Boot 需要权限在 `flink` namespace 创建 FlinkDeployment 和 ConfigMap：

```bash
kubectl apply -f rbac-for-springboot.yaml
```

> `rbac-for-springboot.yaml` 中已绑定 `pre` 和 `prod` namespace 的 `default` ServiceAccount。
> 如果 Spring Boot 使用其他 ServiceAccount 或 namespace，修改文件中的 `subjects`。

### 4. 部署 Spring Boot

按你们现有的 CI/CD 流程部署。

---

## 验证

Spring Boot 启动后，创建并启用一条 CDC 配置，然后检查 K8s：

```bash
# 查看 FlinkDeployment
kubectl get flinkdeployments -n flink

# 查看 Pod（应看到 cdc-{dsName}-{subjectCode} 的 JM 和 TM）
kubectl get pods -n flink

# 查看 JM 日志
kubectl logs -n flink deployment/cdc-{dsName}-{subjectCode}-jobmanager

# 查看 TM 日志
kubectl logs -n flink deployment/cdc-{dsName}-{subjectCode}-taskmanager
```

---

## 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| `ImagePullBackOff` | Operator 镜像缺少 `apache/` 前缀 | `--set image.repository=apache/flink-kubernetes-operator` |
| `no matches for kind "Certificate"` | 未禁用 webhook 且没装 cert-manager | 加 `--set webhook.create=false` |
| `cannot reuse a name that is still in use` | Helm release 已存在 | `helm uninstall flink-kubernetes-operator -n flink` 后再装 |
| Pod 启动后 SQL 报错 | sql-runner.jar 或 Connector 缺失 | 确认 `docker build` 时 `sql-runner.jar` 已存在 |
| Spring Boot 权限报错 | RBAC 未配置或 namespace 不对 | 检查 `03-rbac-for-springboot.yaml` 的 `subjects` |

---

## 清理

```bash
# 删除所有 FlinkDeployment（会连带删除 JM+TM Pod）
kubectl delete flinkdeployments --all -n flink

# 卸载 Operator
helm uninstall flink-kubernetes-operator -n flink

# 删除 namespace
kubectl delete namespace flink
```



kubectl create secret docker-registry harbor-secret \
-n flink \
--docker-server=harbor.cyan.com \
--docker-username=admin \
--docker-password=123456
