---
id: T1661
title: SpillService 幂等复用读面（SpillServiceStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1659
created: 2026-09-15
---

## Question

J 会话第 101 轮：spill 服务层的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：SpillService.tryOffload 的**幂等复用分支**（store 冲突后 isAlreadyStored 同内容→占位复用）零计数——HotTail 每视图重试同 callId 的复用效率不可见。纯服务层轴（R77 是 hook 层判定，本轴是服务内部分支）。

形状裁决：`SpillService` 内静态 `AtomicLong` 四计数——tryOffloadCalls（入口）/ freshStores（新落盘）/ idempotentReuses（幂等复用占位）/ degraded（store 异常降级透传）；嵌套 `record SpillServiceStats` + `stats()` + `resetForTest()`。守恒 `tryOffloadCalls = freshStores + idempotentReuses + degraded + belowThreshold`（belowThreshold=阈值内直返桶）——五计数全谱系。静态面理由同族先例；tryOffload 返回语义逐位不变。

Out of scope：按 toolName/sessionId 分桶（敏感面红线）；preview 截断率（展示面）。
