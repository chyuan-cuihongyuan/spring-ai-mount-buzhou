---
id: T898
title: 预算池借比例上限的裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

ElasticBudgetPool（spec 157）的 surplus 借用无每会话上限——单借方可吃光全部 surplus 饿死同伴。上限怎么定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 25 轮 = effort #600 / spec 624 / impl 477）：

1. `borrowRatio`（K8s LimitRange limit-ratio）：单会话 held ≤ base × ratio；null = 不设限（既有语义零变化）；< 1 拒绝。
2. base=0 会话在设 ratio 时不可借（0 × ratio = 0）——无保底者不可饿死同伴，诚实边界入档（操作面给 base 再给 ratio）。
3. 拒绝与既有 denied 计数同口径。
