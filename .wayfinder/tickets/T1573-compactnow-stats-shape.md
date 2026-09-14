---
id: T1573
title: compact_now 手动压缩判定读面（CompactNowStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1571
created: 2026-09-15
---

## Question

J 会话第 59 轮：memory 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：CompactNowTool（模型主动触发上下文压缩的内置工具）全路径零计数——无会话绑定/无需压缩/压缩失败/压缩成功四路径静默。与 R53 evict_handle 同谱系（模型主动维护行为采用率）：「模型会不会用 compact_now 腾上下文、成功率如何」是压缩工具提示词引导有效性的直接信号（Anthropic /compact 命令采用率思想）。

形状裁决：`CompactNowTool` 内静态 `AtomicLong` 五计数——calls（入口）/ successes（压缩完成）/ skippeds（无需压缩）/ failures（压缩失败，result.error()）/ unboundRejects（会话未绑定）；嵌套 `record CompactNowStats` + `stats()` + `resetForTest()`。守恒 `calls = successes + skippeds + failures + unboundRejects`。静态面理由同族先例；call() 返回语义逐位不变。

Out of scope：foldedMessages 条数分布（结果面在 MicroCompactionResult，调用方可见）；按 sessionId 分桶（敏感面红线）。
