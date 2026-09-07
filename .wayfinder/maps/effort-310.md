# Wayfinder Map — Buzhou 空闲会话后台压缩（effort #310，C 会话第 11 轮）

> C 会话第 11 轮。IdleSessionMonitor「只判定不动作」（179 分层诚实）——空闲
> 判定之后没有动作面：长期空闲的开放会话记忆只增不减，上下文成本白付
> （fog 227「IdleSessionMonitor×压缩/归档动作接线」项）。

## Destination

`IdleCompactionHousekeeper`（buzhou-memory）：会话索引 lastActiveAt 事实 →
空闲超阈 ACTIVE 会话 → ManualCompactor.compact 后台压缩（LSM compaction
思想——空闲即后台维护窗口）。yml `buzhou.memory.idle-compaction.*` 默认关；
每轮批上限防压缩风暴；逐会话隔离失败。

## Notes

- 号段：spec 310 / T611–T612 / impl-333。
- 借鉴：RocksDB/LSM compaction（后台维护窗口）；事实源用 SessionIndex
  （30）而非特征仓（161 per-session 实例不可全局枚举——勘察结论入档）。

## Decisions so far

- 动作注入为 Function&lt;sessionId, CompactResult&gt;（可测性——不绑死
  ManualCompactor 构造复杂度）；装配时传 `compactor::compact`。
- 只压 ACTIVE 空闲会话（CLOSED 归 RetentionSweeper 保留策略族——不越界）。
- 分页枚举上限 10 页（200/页）防大舰队全扫。

## Out of scope

- 归档动作（空闲≠终结——归档语义归保留策略/运维决策）；排水（K8s drain 155 已有）。

## Tickets

- [x] [T611 IdleCompactionHousekeeper（索引事实→动作→隔离/限批/计数）](../tickets/T611-idle-compaction.md)（impl-333）
- [x] [T612 yml 装配 + 四象限回归](../tickets/T612-idle-close.md)（impl-333）
