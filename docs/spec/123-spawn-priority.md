# Spec 123 — spawn 优先级调度（effort #87）

> wayfinder map：`.wayfinder87/MAP.md`（T445–T446）。#85 收口 fog 种子②
> 「优先级调度（SpawnGate）」。借鉴：OS 调度多级队列（priority classes）、
> Envoy 优先级面（高优先级流量先得连接）。

## Problem Statement

spawn 容量闸排队是纯 FIFO（公平信号量）：满载时所有新会话平等排队——但生产场景里
排队者优先级天然不同（运维接管 > 付费租户会话 > 免费批处理会话）。VIP 会话被
批处理大军压在队尾，等价于对最重要的调用方做了劣化。

## Solution

`SpawnGate` 增三级优先级排队（`SpawnPriority`：HIGH / NORMAL / LOW）：

- **抢占规则**：释放的空位有向交接给「最高非空优先级级」的队首——HIGH 排队者在
  NORMAL/LOW 大军之前得到空位；同优先级内严格 FIFO。
- **防加塞**：新到者与在队同级排队者竞争时排到队尾（票据队列有向交接，杜绝
  唤醒抢跑 barging——原公平信号量 `tryAcquire` 竞争窗存在加塞可能，此为修正）。
- **默认等价**：既有 `acquireSlotOrThrow(sessionId)` = NORMAL——全部既有调用
  零行为变化；新重载 `acquireSlotOrThrow(sessionId, priority)` 声明优先级。
- FAIL_FAST 档不排队（无优先级语义）；QUEUE 超时/drain 唤醒/中断/事件词汇全部保持。

## User Stories

1. 作为运维，我对满载 runtime 发起接管会话时声明 HIGH，空位一出现即归我，
   不被排队的批处理会话压尾。
2. 作为宿主，我用既有单参调用时不感知本能力（行为与升级前一致）。
3. 作为宿主，我把批处理任务标记 LOW，让交互会话在争用时可预测地先行。

## Implementation Decisions

- `SpawnGate` 内部从公平 `Semaphore` 改写为 `ReentrantLock` + 每级 `ArrayDeque`
  票据队列 + 每级 `Condition`：释放者锁内选最高非空级队首票据置 granted 并
  signalAll 该级（同级其他等待者醒来见自己票据未授予、继续等待——正确性不依赖
  抢跑顺序）。
- 不变量：available>0 ⇒ 所有队列为空（释放只在无排队者时回增 available）。
- `currentCount()` 口径不变（limit - available，交接在飞瞬间的过渡态不敏感）。
- 不改 `DefaultAgentRuntime`（既有单参调用自动 NORMAL）；配置面（按会话声明
  优先级）后续轮按需。

## Testing Decisions

- 直测 SpawnGate 外部行为（先例 SpawnGateEndToEndTest 为 runtime 级；本能力
  单元面足够）。用 queued 事件做「已入队」同步点，时序确定化。
- 五断言面：HIGH 插队 / 同级 FIFO / 防新到加塞（LOW 先排队、后到默认 NORMAL
  先得——证明默认= NORMAL 且可越级）/ LOW 超时拒绝带等待时长 / 既有六端到端
  测试零回归。

## Out of Scope

- 动态优先级/老化（aging）、按租户自动优先级、runtime yml 配置键。

## Further Notes

- 与 spec 14 spawn 闸正交：容量语义/事件/异常词汇全部不变，只重排等待序。
