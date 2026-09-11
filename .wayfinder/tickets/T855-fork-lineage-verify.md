---
id: T855
title: fork 谱系验证口径（两入口写入、事件计数、导出携带）
type: task
status: closed
assignee: zcode-f
blocked-by: T854
created: 2026-09-12
---

## Question

谱系写入如何钉住不回归？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（SessionForkLineageTest，3 用例全绿；事件捕获经 hook 链——fork 时点早于调用方拿会话引用，listener 注册来不及，hook 链是唯一先于返回的捕获缝）：

- 普通 fork：`state["buzhou.fork.source"]` = 源 id + producer `buzhou.core.fork`；事件带 copiedMessages=2（一问一答）/copiedSummary=false。
- 时间旅行 fork：同样写谱系；事件带 upToTurn + 前缀消息数。
- exportSession 的 state 段含谱系键（跨环境移植不丢）。
- 既有 fork 行为零回归：SessionForkEndToEndTest 5/5（State 不复制语义不受影响——只增一条新键）。
