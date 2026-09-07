# Spec 57 — 共享熔断闸（effort #17）

> wayfinder map：`.wayfinder/maps/effort-17.md`（T254–T258）。OSS 借鉴：LiteLLM Router
> deployment cooldown 跨实例共享（实例标记冷却 → 全实例可见，恢复由探测驱动）。

## Problem Statement

多实例部署时，模型熔断器是进程级状态：每个实例独立统计窗口、独立跳闸、独立冷却。
后果：某 provider 故障时，N 个实例各自烧满自己的失败窗口才跳闸（N 倍无效流量打向
故障方）；冷却后 N 个实例各自探测（N 倍探测流量）；A 实例已确认恢复，B 实例仍 OPEN
拒绝服务（恢复不一致）。

## Solution

「跳闸事实共享、探测与窗口留本地」的分层闸：新增 `CircuitBreakerStateBackend` SPI，
Redis 部署下激活——任一实例跳闸即写共享标记（openedAt / 冷却时长 / 连续跳闸数，TTL=
冷却时长），存活期内全实例对该模型按 OPEN 拒绝；冷却期满（键过期）首见实例转入本地
半开探测（既有槽位/逃生/阈值语义原样）；探测达标任一实例清共享标记 → 全实例恢复。
单进程与内存/JDBC 部署零变化（默认 no-op 后端）。

## User Stories

1. 作为多实例运维者，我希望任一实例跳闸后全实例立即拒绝该模型，所以故障方不再收到 N 倍无效流量。
2. 作为多实例运维者，我希望冷却期满只有少量实例探测，所以恢复期探测流量不放大。
3. 作为多实例运维者，我希望任一实例确认恢复后全实例同步恢复，所以不存在「A 已恢复 B 仍拒绝」的不一致。
4. 作为多实例运维者，我希望连续跳闸退避倍数跨实例延续，所以反复故障的冷却放大不会每实例从头计。
5. 作为单进程用户，我希望行为与现状完全一致，所以升级零风险。
6. 作为 Redis 部署用户，我希望共享标记用 TTL 表达冷却期满，所以无需后台清理任务。
7. 作为 Redis 部署用户，我希望 Redis 不可达时熔断退回本地语义继续工作，所以观测面故障不放大成服务故障。
8. 作为 SDK 开发者，我希望后端是三方法 SPI 且默认 no-op，所以自定义存储接入成本最小。
9. 作为 SDK 开发者，我希望本地状态机（窗口/探测/逃生）原样保留，所以共享只是 CLOSED 分支的额外拒绝源。
10. 作为红队，我希望双 breaker 共享后端的跳闸/拒绝/恢复/冷却边界被钉住，所以分层语义不回退。
11. 作为运维者，我希望共享拒绝仍发 circuit.call-rejected 事件与指标，所以观测面不因共享而失明。
12. 作为 API 治理者，我希望新公共类型登记快照，所以公共面防线不静默漂移。

## Implementation Decisions

- SPI：`CircuitBreakerStateBackend`（resilience 模块）：`recordTrip(modelName, openedAt,
  cooldownMs, consecutiveTrips)`、`activeTrip(modelName)`（存活即返回三元组）、
  `clear(modelName)`；默认实现 no-op（activeTrip 恒空）。
- Redis 实现（store-redis 模块）：键 `<prefix>cb:<模型净化名>`，值
  `trips@openedAtEpochMs`，`PEXPIRE cooldownMs`——键存活 = OPEN，过期 = 可探测。
- 接入点（ModelCircuitBreaker，可选构造参数，null=现状）：
  - `transition(OPEN)` → recordTrip；
  - `transition(CLOSED)`（自 HALF_OPEN 达标）→ clear（幂等）；
  - `admit()` CLOSED 分支先查 activeTrip——活跃按共享 openedAt/cooldown 计算
    retryInMs 拒绝（state=OPEN 口径）；本地 OPEN/HALF_OPEN 分支零变化。
- Redis 故障语义：backend 调用包 try/catch 降级（记 WARN + 继续本地语义）——观测
  面故障不放大（与限流 fail-fast 刻意不同，入档）。
- 激活条件：starter 检测 Redis 后端 bean（与共享限流闸同源条件）；零新配置键。

## Testing Decisions

- 好测试钉外部行为：双 breaker 共享后端的跳闸传导/恢复同步/冷却边界；不测内部锁细节。
- 模块：buzhou-resilience（SPI/接入/no-op 回归）、buzhou-store-redis（嵌入 Redis 的
  TTL 行为）、starter（条件装配面）。
- 先例：RedisRateLimitBackend 双实例红队形态沿用。

## Out of Scope

- 分布式全量状态机（窗口样本/探测在飞跨实例复制）。
- 新配置键；JDBC 后端（需求证据后议）。

## Further Notes

- 共享读写竞窗诚实入档：冷却边界一窗内可能多放行一次探测——探测失败回跳语义兜底。
