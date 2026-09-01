# Spec 310 — 空闲会话后台压缩（effort #310）

> wayfinder map：`.wayfinder310/MAP.md`（T611–T612）。借鉴：RocksDB/LSM
> compaction（空闲即后台维护窗口——fog 227「IdleSessionMonitor×压缩/归档
> 动作接线」项）。

## Problem Statement

空闲判定（179）之后没有动作面：长期空闲的开放会话，历史记忆只增不减——
上下文成本与存储持续白付，而压缩（折叠早前轮次）本可在空闲窗口安全做。

## Solution

`IdleCompactionHousekeeper`（buzhou-memory compact 包，SmartLifecycle）：

- 事实源：会话索引（spec 30）——枚举 ACTIVE 会话，lastActiveAt 早于
  `now - idle-threshold`（默认 1h）者为候选（最久空闲优先）。
- 动作：`ManualCompactor::compact`（与 compact_now 同管线——折叠早前
  完结轮次入结构化摘要）；每轮批上限 `max-per-sweep`（默认 8）防压缩风暴。
- 逐会话隔离失败（失败计数不停轮）；`buzhou.idle-compaction.{compacted,
  skipped,failed, folded-messages}` 计数。
- yml：`buzhou.memory.idle-compaction.{enabled 默认 false, idle-threshold,
  interval 默认 10m, max-per-sweep}`；分页枚举上限 10 页防大舰队全扫。

## User Stories

1. 作为运维，午休/过夜空闲会话自动瘦身——次日回来上下文成本已降。
2. 作为运维，批上限与失败隔离——千会话同时空闲也不起压缩风暴。

## Implementation Decisions

- 动作注入 Function（可测性）；事实源用索引而非特征仓（per-session 实例
  不可全局枚举——勘察结论）。
- 只动 ACTIVE 空闲（CLOSED 归 RetentionSweeper——不越界）。

## Testing Decisions

- `IdleCompactionHousekeeperTest`（内存索引 + 记录型动作函数）：空闲 ACTIVE
  压到 / 新鲜与 CLOSED 不动 / 批上限 / 失败隔离与计数。
- 装配测试：enabled=false 无 bean；enabled + 依赖齐（compactor/index）装配。

## Out of Scope

- 归档动作（空闲≠终结）；排水；特征仓事实源迁移。

## Further Notes

- 维护窗族：排水（155）/ 维护门（205）/ 保留策略（RetentionSweeper）/
  **空闲压缩（本轮）**。
