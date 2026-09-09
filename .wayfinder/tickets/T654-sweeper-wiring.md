---
Type: task
Status: closed
---
## Question

RetentionSweeper 选主门（非 leader 跳周期留计数/停机让位/Redis 异常
跳过/手动不设门）+ yml 装配面（buzhou.leader-election.{enabled,ttl,
holder-id}）+ README/快照收口。

## Resolution

done（2026-09-04）：impl-354；sweeper 增 15 参构造器（旧双构造器委托
null——二进制兼容），调度周期先取续再执行；stop() 顺带 resign；计数
buzhou.retention.skipped-not-leader。store-redis 配 LeaderElectionProperties
+ ConditionalOnProperty 装配（store.type=redis 且 enabled）；core 装配
经 ObjectProvider 注入（无 bean 零变化）。门 5 用例 + 装配 2 用例绿；
快照 regenerate +3 型；README 纵深 IV 加行、覆盖门绿。
