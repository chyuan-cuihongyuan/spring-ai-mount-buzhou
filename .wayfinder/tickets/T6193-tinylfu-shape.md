---
id: T6193
title: S 会话 S47 TinyLFU Admission 准入策略的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-25
---

## Question

缓存准入怎么让扫描键不挤掉热键且历史会淡出？（spec 5046 /
effort #5046 / S47）

## Resolution

**TinyLfuAdmission<K>（core/cache）**：Caffeine W-TinyLFU
准入面——4 位饱和 count-min 双行 sketch（估计取行最小），
admit 候选≥受害者才准入；记录达阈值全体减半老化；
estimate/resetCount/sinceReset 读数；畸形 fail-fast。
