---
id: T1153
title: 注入检测分级策略的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

paranoia 分级做进 classifier 还是独立裁决映射？观察带语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 27 轮 = effort #826 / spec 826 / impl 579）：`InjectionParanoiaPolicy` 独立纯函数——L1-L4 标准阈值表+BLOCK/LOG/ALLOW 三态（LOG=阈值下 0.10 观察带）；分数截断；≥ 边界；null fail-fast；classifier 零变更。
