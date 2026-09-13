---
id: T1529
title: 摘要桥操作与代数回退读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 38 轮：摘要桥操作与代数回退读面在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 38 轮 = effort #1038 / spec 1038 / impl 790）：缺口成立——SummaryStoreBridge（spec 20/95 摘要版本化存取桥）save/loadLatest 全程零计数，且**代数回退无探测**：generation 本应单调不减（版本化追加语义），回退即摘要被旧快照覆盖的异常信号（R28 手动压缩/R21 自动压缩共用本桥——双写方竞争的唯一可视化点）。落点 buzhou-memory summary 包：实例级 saves/loads/generationRegressions 三 AtomicLong + per-session lastGeneration 有界 LRU（1024——TurnTimingHook 同先例）+ 嵌套 record `SummaryStoreStats(saves, loads, generationRegressions)` + `stats()`。save/loadLatest 返回值与存储语义逐位不变。
