# Wayfinder Map — Buzhou 选主扩散：归档清理与空闲压缩（effort #341，C 会话第 42 轮）

> C 会话第 42 轮。331 立了选主门但只接了 RetentionSweeper——同类的
> ArchivePurgeJob（归档 TTL 清扫）与 IdleCompactionHousekeeper（空闲会话
> 压缩）在多实例下仍是每实例各自跑。331 明示「其他后台任务接线等模式
> 验证后再扩散」——模式已验证（331 落地+全绿），本轮兑现。

## Destination

两后台任务接同一 LeaderElector 门（331 同法）：调度周期先取续——
非 leader 跳周期留计数、异常跳过（失联宁可少做）、stop 主动让位、
手动触发不设门；构造器加重载（旧构造器委托 null——二进制兼容）；
装配经 ObjectProvider 注入（无 bean 零变化）。

## Notes

- 号段：spec 341 / T673–T674 / impl-364。
- 借鉴源：K8s leader election（331 同源扩散轮）。
- 纪律：三个家务任务共用一个 scope（buzhou:leader:housekeeping）——
  一个 leader 管全部家务，不三头选举。

## Decisions so far

- 门内计数各自命名（buzhou.archive.skipped-not-leader /
  buzhou.idle-compaction.skipped-not-leader）。

## Out of scope

- 分任务分 scope 选举（一个家务 leader 足够——分域等真需求）；
- ArchivePurgeJob 与 RetentionSweeper 的调度合并（各自 SmartLifecycle
  生命周期独立）。

## Tickets

- [x] [T673 ArchivePurgeJob 选主门](tickets/T673-archive-gate.md)
- [x] [T674 IdleCompactionHousekeeper 选主门 + 装配 + 收口](tickets/T674-compaction-gate.md)
