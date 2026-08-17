---
Type: task
Status: closed
blocked-by: T136
---
## Question

黄金轨迹 D：迁移/导出扩展段/health 新维度的轨迹（迁移双向、facts 段往返已在演示——提升为黄金编号）。

## Resolution

AFK 自决：并入 GoldenTrajectoryEffort7Test 或新类两轨迹：①跨 runtime 迁移（H2→内存 keepIds/重映射两型）；②health 详情含 outbox 水位（pending/deadLetters 数值可见）。产 spec 38 §D + impl-116（与 T137 可合并执行）。
