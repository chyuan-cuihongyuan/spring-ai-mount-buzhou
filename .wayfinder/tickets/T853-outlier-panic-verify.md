---
id: T853
title: 恐慌阈值验证口径（边界取舍向上、恰在阈值、默认关闭回归）
type: task
status: closed
assignee: zcode-f
blocked-by: T852
created: 2026-09-12
---

## Question

panic 阈值的边界语义如何钉住才不回归？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（ModelOutlierEjectionPanicTest，8 用例全绿，buzhou-resilience 222/222）：

- 默认（0=关）：全逐即空池——既有行为零变化。
- panic=100 + 全逐：返回全量候选。
- 3 选 1 逐 + 50%：ceil(1.5)=2，健康 2 ≥ 2 → 不恐慌、保序剔除。
- 4 选 4 逐 + 50%：健康 0 < 2 → 恐慌返回全量。
- 恰在阈值（1/2 健康 + 50%）不恐慌：严格低于才触发。
- 空候选恒空；percent 越界（-1/101）构造拒绝；两参构造 = 0、withPanicAll = 100。
