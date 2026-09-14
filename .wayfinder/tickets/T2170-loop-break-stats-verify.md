---
id: T2170
title: 打断分布计数与水位单调的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2169
created: 2026-09-14
---

## Question

如何证明 per-tool 计数、游程水位与换参重置语义？

## Resolution

**用户常设授权 AFK（可推翻）**

`ToolLoopBreakerBreakStatsTest` 三测全绿（`mvn -pl buzhou-core -am test`）：5 连发打断 3 次+maxRun=5+per-tool 计数；换参/换工具重置后双工具各计 1；**reset 清分布不清会话 run 状态**（清后仍处打断裂缝、maxRun 水位含历史 5）。评审修正：直调替身缺 SessionStateHandle import 编译错修正。
