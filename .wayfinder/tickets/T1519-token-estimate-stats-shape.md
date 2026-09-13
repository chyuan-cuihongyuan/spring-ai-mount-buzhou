---
id: T1519
title: token 估算调用量与总量读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 34 轮：token 估算调用量与总量读面在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 34 轮 = effort #1033 / spec 1033 / impl 786）：缺口成立——CharHeuristicTokenEstimator（4 字符/token 启发式，预算计算的事实基准）estimate/estimateMessages 全程零计数：**预算面吃的估算总量与调用量**不可见——估算调用量突增即预算路径被高频触发的信号（配合 spec 1027 压缩水位定位压缩饥饿）。落点 core/token：静态进程级 estimateCalls/batchCalls/totalEstimatedTokens 三 AtomicLong + 嵌套 record `TokenEstimateStats` + `stats()`/`resetForTest()`（静态先例：R1 ToolPolicyMatcher/R15 Spotlighting）；调用点内联构造故实例计数无意义——进程级口径。行为逐位不变。
