---
id: T1154
title: 注入检测分级策略验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1153]
created: 2026-09-13
---

## Question

阈值表/三带分化/边界/截断如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 27 轮 = effort #826）：InjectionParanoiaPolicyTest 6 例——四档阈值表/同分数四级分化+L3 观察带（首跑预期修正：0.72≥0.70 应 BLOCK）/观察带三界/边界相等/越界截断+L4 下界/null fail-fast。
