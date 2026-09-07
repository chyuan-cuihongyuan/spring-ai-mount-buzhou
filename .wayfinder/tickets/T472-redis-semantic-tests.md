---
Type: task
Status: closed
---
## Question

容器测试五面：跨实例命中 / 阈值下 miss / 惰性过期清除 / 容量驱逐最旧 / 后端不可达旁路。

## Resolution

done（2026-08-30）：impl-275；RedisSemanticVectorCacheContainersTest（无 Docker 跳过，
CI 生效）五测试 + 本地编译验证。
