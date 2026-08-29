---
Type: task
Status: closed
---
## Question

舱占用跨实例共享：心跳 TTL 事实 + 集群聚合查询。

## Resolution

done（2026-08-30）：impl-276；core.spi.BulkheadStateBackend（默认 no-op）+
InMemoryBulkheadStateBackend（惰性过期清扫）+ store-redis RedisBulkheadStateBackend
（HASH 心跳 + 键 TTL 兜底，故障降级空快照）。
