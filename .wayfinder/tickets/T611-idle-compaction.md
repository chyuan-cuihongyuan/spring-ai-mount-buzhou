---
Type: task
Status: closed
---
## Question

IdleCompactionHousekeeper：索引事实 → 空闲 ACTIVE 候选 → 压缩动作（限批/
隔离/计数/分页上限）。

## Resolution

done（2026-09-01）：impl-333；SmartLifecycle 周期 sweepOnce + Function 动作注入
+ 四计数。IdleCompactionHousekeeperTest 四用例绿。
