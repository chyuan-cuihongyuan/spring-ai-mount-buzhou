---
id: T2140
title: 分档阈值边界与畸形对哨兵的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2139
created: 2026-09-14
---

## Question

如何证明阈值边界归属、畸形哨兵与 tightest 排序？

## Resolution

**用户常设授权 AFK（可推翻）**

`BudgetTierClassifierTest` 五测全绿（`mvn -pl buzhou-core -am test`）：阈值边界精确（79=GREEN/80=WARN/99=WARN/100=HARD 含端点）；四桶计数；tightest 饱和度降序（99%/90% 前二）；畸形对 UNKNOWN+ratio=-1 不冒充；越限 250% 仍 HARD（ratio=2.5 不封顶）。
