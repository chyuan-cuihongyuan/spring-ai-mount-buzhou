# 1041 — time-travel fork 操作计数读面

> 来源：J 会话第 41 轮 = effort #1041（[T1535](../../.wayfinder/tickets/T1535-fork-stats-shape.md) / [T1536](../../.wayfinder/tickets/T1536-fork-stats-verify.md) / impl 793）。借鉴：LangGraph [get_state_history/fork](https://langchain-ai.github.io/langgraph/)（time-travel 使用统计——fork 了几个检查点、复制了多少状态）。

## Problem Statement

SessionForks.forkFrom（spec 12 §core-6 time-travel 分叉）执行零计数：fork 了多少次、每次复制多少消息不可见——time-travel 使用水位（功能是否被用、用得多频繁）无读数；fork 后消息复制量的异常（复制 0 条=空 fork）亦无信号。listCheckpoints 谱系读面已覆盖谱系游走（spec 711），缺的是操作计量。

## 目标

- `SessionForks` 增量（buzhou-memory，实例级）：`forksCreated` / `messagesCopied` 两 AtomicLong——forkFrom 每次执行 forksCreated+1、messagesCopied 累加复制条数。
- 嵌套 record `ForkStats(long forksCreated, long messagesCopied)` + `stats()` 快照。
- forkFrom 返回值（新 sessionId）与原会话不动语义逐位不变。

## 兼容性

纯增量读面；无新配置项。

## Out of Scope

- 谱系深度/环检测（spec 711 ForkLineageWalker 已覆盖）。
- 跨进程聚合。
