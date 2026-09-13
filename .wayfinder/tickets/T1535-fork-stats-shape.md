---
id: T1535
title: time-travel fork 操作计数读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 41 轮：time-travel fork 操作计数读面在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 41 轮 = effort #1041 / spec 1041 / impl 793）：缺口成立——SessionForks.forkFrom（spec 12 §core-6 time-travel 分叉）零计数：fork 执行了多少次、每次复制多少消息不可见——time-travel 使用水位（LangGraph get_state_history/fork 的使用统计思想）无读数；listCheckpoints 已是读面（复用不重复建设）。落点 buzhou-memory：SessionForks 实例级 forksCreated/messagesCopied 两 AtomicLong + 嵌套 record `ForkStats(forksCreated, messagesCopied)` + `stats()`；forkFrom 返回值（新 sessionId）逐位不变。实例级；嵌套类型不动 API 快照。
