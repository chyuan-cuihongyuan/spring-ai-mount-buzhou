---
Type: task
Status: closed
---
## Question

Reporter（本地发布 + 集群快照）与两实现回归。

## Resolution

done（2026-08-30）：impl-276；AgentBulkheadReporter（report/clusterSnapshot）+
core 单测（双实例聚合/过期剔除/本地并入）+ redis 容器测试（无 Docker 跳过）绿。
