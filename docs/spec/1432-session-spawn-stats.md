# 1432 — 会话 spawn 统计读面

> 来源：L 会话第 33 轮 = effort #1432（票 T2165 / T2166 / impl 1083）。借鉴：HikariCP 连接池建连统计（attempts/collisions 是池治理第一读数——冲突频次决定 id 规划与容量）。

## Problem Statement

`DefaultAgentRuntime.spawn` 的冲突拒绝（`SessionAlreadyActiveException`——同 id 租约被持）与 `steal` 抢占路径零读面：spawn 失败率、抢占频次、活跃会话峰值（容量规划）全无数据——「同 id 冲突高发」这种 id 规划错误静默。

## 目标

- `SessionSpawnStats`（core/session，进程级静态读面，ToolArgsValidator 同款先例——埋点在 internal runtime，读面归公共类）：
  - 漏斗：`attempts`（doSpawn 入口）/ `successes`（spawn 完成，含 steal 成功）/ `collisions`（SessionAlreadyActiveException）/ `steals`（抢占路径执行数）；
  - 守恒式：`attempts = successes + collisions`（steal 成功是 successes 的完成形态）；
  - `activePeak`：spawn 时点观测的活跃会话峰值水位（口径显式：仅 spawn 路径采样，非全时段）；
  - `stats()` 嵌套 `record Snapshot` + `resetForTest()`。
- DefaultAgentRuntime.doSpawn 四点埋点只增记账，spawn/租约/steal 语义逐位不变。

## 兼容性

纯增量读面：spawn/租约/steal/容量闸（SpawnGate）语义逐位不变。

## Out of Scope

- 全时段 active 峰值（需 close 路径同步采样——口径复杂化另轮）。
- 按appId/agent 分桶（基数红线）。
- SessionCapacityExceededException 容量闸计数（SpawnGate 域 spec 831 已有拒绝分布）。
