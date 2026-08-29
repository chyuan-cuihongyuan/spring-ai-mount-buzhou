# Wayfinder Map — Buzhou 跨实例舱占用聚合（effort #92，B 会话第 4 轮）

> B 会话第 4 轮。fog 种子④「跨实例分布式舱/表聚合」：AgentBulkhead 占用是实例本地
> 事实（spec 84），健康面只见本实例——集群视角的舱压力不可见。

## Destination

舱占用跨实例聚合：BulkheadStateBackend SPI（心跳 TTL 共享事实）+ InMemory/Redis
两实现 + AgentBulkheadReporter（本地心跳发布 + 集群快照查询）。借鉴 spec 57
CircuitBreakerStateBackend「共享事实不共享状态机」范式。

## Notes

- 故障语义：观测面后端不可达 = 降级空快照 + WARN（不放大为服务故障——spec 57 同款）。
- A 会话已写 spec 126（prefix-cache）——B 预告清单中「前缀缓存命中率」让位，
  B 后续补位主题：工具调用合并去重（Hystrix request collapsing）。

## Decisions so far

- 心跳模型而非共享信号量：舱许可授予仍是本地低延迟路径，跨实例只聚合观测事实。

## Not yet specified

- 健康面 /actuator 段接线（本轮先查询 API）；定时心跳 autoconfig。

## Out of scope

- 沿用 #7–#91；全局集群舱（跨实例许可协调）——延迟与正确性代价不成比例。

## Tickets

- [x] [T473 BulkheadStateBackend SPI + InMemory/Redis 心跳实现](tickets/T473-bulkhead-cluster.md)（impl-276）
- [x] [T474 AgentBulkheadReporter + 聚合回归](tickets/T474-bulkhead-cluster-tests.md)（impl-276）
