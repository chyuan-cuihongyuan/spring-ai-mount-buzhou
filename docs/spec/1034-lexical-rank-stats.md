# 1034 — 词法排序生效计数读面

> 来源：J 会话第 35 轮 = effort #1034（[T1521](../../.wayfinder/tickets/T1521-lexical-rank-stats-shape.md) / [T1522](../../.wayfinder/tickets/T1522-lexical-rank-stats-verify.md) / impl 787）。与 R17 同谱系：机制是否真的在起作用的**生效显形**（排序器空转探测）。

## Problem Statement

LexicalSkillRanker（spec 605 BM25 词法排序，opt-in）rank 返回重排序后的候选清单，但零计数：排序运行了多少次、**输出序与输入序实际不同**多少次不可见——若问法与技能描述词法永不相交，排序器空转（每次运行都原序返回）且无任何信号。

## 目标

- `LexicalSkillRanker` 增量（buzhou-skill，实例级）：`runs`（有效 BM25 运行数：候选>1 且问法非空）/ `reordered`（输出序 ≠ 输入序的运行数）两 AtomicLong。
- 嵌套 record `RankStats(long runs, long reordered)` + `stats()` 快照——reordered/runs 长期近零 = 词法路空转信号。

## 兼容性

纯增量读面：rank 返回值逐位不变；无新配置项。

## Out of Scope

- 命中分布按技能分桶（与 I 池使用统计分轴回避）。
- BM25 参数可配（诚实边界维持经典值）。
