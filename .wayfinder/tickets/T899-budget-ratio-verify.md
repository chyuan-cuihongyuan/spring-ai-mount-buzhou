---
id: T899
title: 借比例上限验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T898
created: 2026-09-12
---

## Question

上限语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（ElasticBudgetPoolRatioTest 3/3 + core 全模块零回归）：

- ratio=2：恰 base×2 放行、+1 拒（surplus 充足也拒）；同伴各自上限互不影响。
- 默认 null：吃光 surplus 旧语义不变。
- base=0 × ratio 不可借；ratio<1 构造拒绝。
