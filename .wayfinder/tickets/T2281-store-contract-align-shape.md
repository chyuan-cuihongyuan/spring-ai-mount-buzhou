---
id: T2281
title: 降级存储契约对齐 + 机制计数口径统一的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 17 轮：design-incompleteness 六-1（DegradingObservabilityStore 双份分叉）与七-1（机制计数矛盾）如何处置？

## Resolution

**用户常设授权 AFK（可推翻）**

① 六-1：redis 版 runDegradable 补 buzhou.store.write.failures{policy=degrade} 指标（jdbc 版 impl-41/spec 13 §T66 先例同款）——同名降级策略两库观测行为一致的契约漂移修复；
② 七-1：README「九大机制」升「十大机制」（表加第 10 行模型韧性层——resilience 独立模块独立机制，与 CLAUDE.md 十大口径统一；评审建议二选一取「README 升」——韧性层已是生产级纵深的主力域）。
