---
id: T2136
title: 舱壁峰值水位与饱和度的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2135
created: 2026-09-14
---

## Question

如何证明峰值单调、饱和度口径与拒绝不虚高？

## Resolution

**用户常设授权 AFK（可推翻）**

`AgentBulkheadPeakTest` 四测全绿（`mvn -pl buzhou-core -am test`）：串行占席峰值=2 且释放不回退；饱和度=峰值/上限（0.5 精确）；无限舱峰值 0+哨兵 -1；拒绝路径不虚高（打满后 QUOTA_EXCEEDED、峰值仍 1、饱和度 1.0）。AgentBulkheadTest/ResizeTest 回归 10 测绿。
