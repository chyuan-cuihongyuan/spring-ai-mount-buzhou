---
id: T2995
title: 多类型载荷预算分账的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

多类型共享预算的水填分配怎么算？（spec 1897 / effort #1897 / R98）

## Resolution`

**加权水填纯计算 `PayloadBudgetSplit`（core/prompt）**：allocate
（公平份额迭代、需求 ≤ 份额取需求退出、余量回流、零头诚实披露）
→ Allocation{grants,leftover}。budget≥0/demand≥0/weight≥1/同长
fail-fast。落轮 grep 复核抑制规则（330）占坑换静脉。
