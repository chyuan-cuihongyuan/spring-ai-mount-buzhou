---
id: T1561
title: evict_handle 逐出判定读面（EvictStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1559
created: 2026-09-15
---

## Question

J 会话第 53 轮：spill 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题（跨模块轮换入 spill 域，EvictHandleTool 全路径零计数）：模型主动逐出（evict_handle）的成败分布不可见——模型是善用逐出腾上下文、还是反复给非法路径（学习失败信号）。Anthropic「清除已消费 tool_result」采用率思想。

形状裁决：`EvictHandleTool` 内静态 `AtomicLong` 四计数——attempts（call 入口）/ evictions（成功 markEvicted）/ badPathRejects（非 spill:// URI）/ parseRejects（解析错误 catch 兜底）；嵌套 `record EvictStats` + `stats()` + `resetForTest()`。守恒 `attempts = evictions + badPathRejects + parseRejects`。静态面理由同 R46–R52 先例。call() 返回语义逐位不变。

Out of scope：按 path 分桶（敏感面红线）；墓碑收缩效果计量（视图生成侧，另轴）。
