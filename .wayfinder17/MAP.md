# Wayfinder Map — Buzhou 共享熔断闸（effort #17）

> effort #17（已闭合 2026-08-29），延续 #5–#16；收口后累计 163 轮 / T1–T258 / impl 1–201。
> 本 effort 主线：**熔断状态跨实例共享**——fog 毕业生：进程级熔断在多实例部署下每实例
> 独立跳闸（N 倍探测流量打向故障 provider）。借鉴 LiteLLM（~26K★）Router 的
> deployment cooldown 共享思想（实例 A 标记冷却 → 全实例可见）；本仓适配为「跳闸事实
> 共享、探测与窗口留本地」的分层闸（探测在飞是实例本地事实，共享全量状态机不可行也不必要）。

## Destination

Redis store 部署 + 熔断启用时：任一实例跳闸（OPEN）→ 全实例对该模型拒绝调用（共享
冷却窗 openedAt/cooldown/trips 落 Redis TTL 键）；冷却期满首见实例转本地半开探测，
探测达标任一实例清除共享标记 → 全实例恢复 CLOSED。单进程/内存/JDBC 部署行为零变化
（共享闸仅 Redis 部署激活，与共享限流闸同口径）。零新配置键。

## Notes

- 领域/测试哲学/10K★ 政策/AFK 授权：沿用 effort #6–#16 MAP Notes。
- 外部事实源：LiteLLM Router cooldown——失败 deployment 进入 cooldown，Redis 模式下
  跨实例共享；恢复由探测驱动。本地裁定：只共享「OPEN 事实 + 冷却参数 + 连续跳闸数」，
  窗口样本/在飞探测留本地（共享它们 = 分布式状态机，复杂度不成比例）。
- 本地勘察（2026-08-29）：`ModelCircuitBreaker` 三态 + 监视器锁 + Clock 注入；
  `transition(OPEN/CLOSED)` 是共享写点，`admit()` CLOSED 分支是共享读点；
  `RateLimitBackend` SPI（内存/Redis 两实现 + starter 条件装配）是现成分层范式；
  RedisTTL 键天然表达「冷却期满」（键过期 = 可探测）。
- 诚实边界：共享标记读写非事务（读-判-写竞窗）——最坏后果是冷却边界一窗内某实例
  多放行一次探测（探测本身有失败回跳语义兜底）；本地窗口不跨实例聚合（每实例独立
  判定跳闸，共享只放大跳闸事实——保守方向正确）。
- 过程教训沿用：examples 依赖改动后全量 install；新公共类型必须 regenerate 快照。

## Decisions so far

- **共享面 = 跳闸事实三元组**（openedAt / effectiveCooldownMs / consecutiveTrips）+
  Redis TTL = 冷却时长（键存活期内全实例视为 OPEN；过期即可探测）。
- **探测本地化**：首见冷却期满的实例走本地 HALF_OPEN（既有槽位/逃生/阈值语义不动）；
  达标 transition(CLOSED) 时 DEL 共享键（幂等）。
- **后端 SPI `CircuitBreakerStateBackend`**：`recordTrip / activeTrip / clear` 三方法；
  默认 no-op（进程语义零变化）；Redis 实现（TTL 键）+ starter 条件装配（有
  RedisRateLimitBackend 同源 bean 条件）。
- **本地跳闸优先**：本地状态机完整保留——共享标记只作为 CLOSED 分支的额外拒绝源
  （本地 OPEN/HALF_OPEN 语义不变）。

## Not yet specified

- RunawayHook / TokenBudgetHook 计数写路径同型原子化（fog 沿用）。
- outbox SCAN 下推 / 观测 OLAP / skill 语义排序（fog 沿用）。
- 共享熔断的跨实例窗口聚合（共享样本计数）——量级证据后议（当前保守方向已正确）。

## Out of scope

- 沿用 effort #7–#16 Out of scope 全部条目。
- 分布式全量熔断状态机（窗口/探测在飞跨实例复制）。
- 新配置键（激活条件 = Redis 后端 bean 存在，同共享限流闸口径）。

## Tickets

初始 5 张（T254–T258，按轮逐张闭合）：

- [x] [T254 CircuitBreakerStateBackend SPI + Redis TTL 实现](tickets/T254-cb-backend.md)（impl-199；core.spi 分层同 RateLimitBackend）
- [x] [T255 ModelCircuitBreaker 接入共享闸（跳闸写/恢复清/CLOSED 分支拒）](tickets/T255-cb-wiring.md)（impl-199；四参构造重载 + 拒绝公共路径抽出）
- [x] [T256 双实例红队 + 单测（A 跳闸 B 拒 / 恢复全放 / 单进程零变化）](tickets/T256-cb-redteam.md)（impl-200；SharedCircuitGateTest 5 例 + Redis TTL 后端 5 例）
- [x] [T257 文档面（runbook §6 / CONTEXT 术语 / 快照登记新类型）](tickets/T257-cb-docs.md)（impl-201；快照 +2 类型 + api-surface.md 两节）
- [x] [T258 里程碑 verify + 收口](tickets/T258-effort17-closing.md)（全仓 verify 绿；累计 163 轮）
