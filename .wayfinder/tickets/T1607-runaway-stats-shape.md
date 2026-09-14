---
id: T1607
title: Runaway 预算 hook 判定读面（RunawayStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1605
created: 2026-09-15
---

## Question

J 会话第 76 轮：core/runaway 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：RunawayHook.beforeModel（预算 runaway 主判定：wall-clock/轮步数/会话步数三硬顶）分支零计数——终止频次与放行量不可见，预算配置过紧/过松均无量化信号。

形状裁决：`RunawayHook` 内静态 `AtomicLong` 四计数——invocations（入口）/ blocked（三硬顶出口合桶）/ allowed（正常放行）/ disabledSkips（机制关闭）；嵌套 `record RunawayStats` + `stats()` + `resetForTest()`。守恒 `invocations = blocked + allowed + disabledSkips`。静态面理由同族先例；beforeModel 返回语义逐位不变。

Out of scope：按硬顶维度分桶（reason 已在事件 payload）；软阈值事件统计（既有语义）。
