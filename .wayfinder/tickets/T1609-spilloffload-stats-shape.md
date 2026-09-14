---
id: T1609
title: Spill 溢出 hook 判定读面（SpillOffloadStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1607
created: 2026-09-15
---

## Question

J 会话第 77 轮：spill 域主 hook 的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：SpillOffloadHook（超阈值工具输出溢出落盘主 hook）判定路径零计数——溢出触发率、durable 覆盖跳过、降级 refrain 动作分布不可见。溢出触发率是 Spill 管线容量的第一信号（logrotate 轮转率思想）。

形状裁决：`SpillOffloadHook` 内静态 `AtomicLong` 六计数——invocations（afterTool 入口）/ durableSkips（durable 覆盖永不溢出）/ errorSkips（error/null 结果）/ cleanInline（阈值内全量内联）/ offloaded（溢出替换，含数组逐项场景合计）/ refrains（OnFail.REFRAIN 降级替换）；嵌套 `record SpillOffloadStats` + `stats()` + `resetForTest()`。守恒 `invocations = durableSkips + errorSkips + cleanInline + offloaded + refrains`。静态面理由同族先例；afterTool 返回与替换语义逐位不变。

Out of scope：按 toolName 分桶（SpillThresholds 配置面）；溢出字节数（store 层既有口径）。
