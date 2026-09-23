---
id: T6026
title: R 会话 R13 区块剪枝的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6025]
created: 2026-09-23
---

## Question

R13 合同怎么逐一验绿？（spec 4012 / effort #4012 / R13）

## Resolution

**验证通过**：ZoneMapPrunerTest 五测全绿——三块点查命中/沟里全剪/
边界含等；区间剪枝比率 1/3 与全剪 1.0；擦边双侧（31–35 全剪 vs
30–40 恰触边两块保守读）；空值面 z2 显形；畸形六型 fail-fast。
