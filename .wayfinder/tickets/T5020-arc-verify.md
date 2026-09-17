---
id: T5020
title: Q 会话 R10 ARC 缓存的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5019]
created: 2026-09-18
---

## Question

R10 合同怎么逐一验绿？（spec 3009 / effort #3009 / R10）

## Resolution

**验证通过**：AdaptiveReplacementCacheTest 九测全绿——miss null/
hit 值、50 op 容量恒 ≤5、同键更新不增、c=2 新近晋升（get 者幸存）、
c=3 B1 幽灵 p 0→1 手迹、c=10 扫描抗性 40 键扫描后热键 ≥9 存活
（LRU 全灭对照）、随机 500 op p∈[0,4] 恒界、幽灵恒 ≤capacity、
容量 0/负 fail-fast。
