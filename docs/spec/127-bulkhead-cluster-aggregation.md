# Spec 127 — 跨实例舱占用聚合（effort #92）

> wayfinder map：`.wayfinder92/MAP.md`（T473–T474）。#85 收口 fog 种子④
> 「跨实例分布式舱/表聚合」。范式先例：spec 57 CircuitBreakerStateBackend
> （共享事实、不共享状态机；观测面故障不放大为服务故障）。

## Problem Statement

AgentBulkhead（spec 84）的占用是实例本地事实：每实例各自限 Turn 并发没错（许可
授予必须本地低延迟），但运维想知道「agent alpha 现在全集群在飞多少 Turn / 分摊在
几个实例」时，健康面只答得出本实例数字——集群视角的舱压力（是否该扩容、热点是否
全局）不可见。

## Solution

「心跳共享事实、许可留本地」三层：

- **SPI（core.spi.BulkheadStateBackend）**：`heartbeat(InstanceOccupancy, ttl)`
  ——实例对每 agent 发布（instanceId, occupied, limit, at）；`clusterOccupancy()`
  ——agent → 全实例活跃心跳占用求和（过期心跳不算）。默认全 no-op（单进程零变化）。
- **InMemory 实现（core）**：ConcurrentHashMap + 惰性过期清扫（读时剔除）——
  单进程/测试默认。
- **Redis 实现（store-redis）**：单 HASH 键心跳（field=`agent|instance`，value=
  JSON 含 expireAt；键 TTL 兜底回收）；故障降级空快照 + WARN（不 fail-fast——
  观测面风险不对称，spec 57 同款）。
- **AgentBulkheadReporter（core）**：`report()` 把 AgentBulkhead.global() 全部
  配置舱的当前占用发布到后端（实例 id 宿主给）；`clusterSnapshot()` 返回
  本地实时 + 远端心跳的合并视图（agent → 占用/实例数）。

## User Stories

1. 作为运维，我在任一实例查 clusterSnapshot 即得每 agent 的全集群在飞 Turn 数与
   承载实例数——扩缩容决策有全局依据。
2. 作为宿主，单进程部署（默认 no-op 后端）零行为零开销变化。
3. 作为运维，Redis 抖动时舱限流不受影响（许可本地裁决），仅集群视图暂盲（降级
   空快照 + WARN，恢复后自愈）。

## Implementation Decisions

- 心跳而非共享信号量：跨实例许可协调的延迟/竞争代价与收益不成比例（诚实边界：
  集群总占用可能瞬时超过各实例上限之和的配额想象——本能力是观测聚合，不是全局闸）。
- field 编码 `agent|instance`（两者各含 `|` 视为非法——fail-fast 参数校验）。
- 定时发布（@Scheduled autoconfig）与 /actuator 健康段接线后续轮按需。

## Testing Decisions

- core：InMemory 双实例心跳聚合 / TTL 过期剔除 / Reporter 并入本地实时占用 /
  no-op 后端零影响。redis：容器测试（无 Docker 跳过）跨实例求和 + 过期剔除。
- 先例：CircuitBreakerStateBackend 单测 + RedisCircuitBreakerStateBackend 容器测试。

## Out of Scope

- 全局集群舱（跨实例许可协调）；成本台账跨实例聚合（spec 65 已 CAS 共享）。

## Further Notes

- 与 spec 84/92 正交：本地舱语义/健康面不变，本层只加集群观测。
