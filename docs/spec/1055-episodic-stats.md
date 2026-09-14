# 1055 — 情景记忆读写双守恒读面

> 来源：J 会话第 55 轮 = effort #1055（[T1565](../../.wayfinder/tickets/T1565-episodic-stats-shape.md) / [T1566](../../.wayfinder/tickets/T1566-episodic-stats-verify.md) / impl 807）。借鉴：mem0 episodic memory 命中率（情景库写读两侧的分布是增益机制是否真实生效的第一信号）。J 会话首个 memory 域轮。

## Problem Statement

`EpisodeLedger`（impl-38 / spec 13 §growth-8）全路径零计数——record 静默丢弃（embedding provider 缺失 / goal 空白）、持久化异常「增益非主链路」吞掉、召回空结果、few-shot 无命中 empty：**情景记忆的写入量、召回命中率完全不可见**。宿主无法回答「情景库在被真实写入吗、few-shot 注入几轮命中一次」——经验复用机制是否空转无从判断。

## 目标

- `EpisodeLedger` 增量（memory/episodic，静态面）：九 `AtomicLong` 双守恒。
  - 写侧：`recordCalls` / `recorded`（写入成功）/ `recordDropped`（provider 缺失或 goal 空白静默丢）/ `recordFailures`（持久化异常）——守恒 **recordCalls = recorded + recordDropped + recordFailures**；
  - 读侧：`recallCalls`（recallExamples 入口，fewShotBlock 复用同点）/ `recallHits`（结果非空）/ `recallEmpties`（有效调用但空——含全被 0.10 地板滤掉）/ `recallDropped`（provider 缺失或 goal 空白）——守恒 **recallCalls = recallHits + recallEmpties + recallDropped**。
- 嵌套 `record EpisodicMemoryStats(...)` + `stats()` + `resetForTest()`。

## 兼容性

纯增量读面：record/recallExamples/fewShotBlock 返回语义、0.10 语义地板、持久化恢复序号逐位不变；fewShotBlock 经 recallExamples 走 recall 侧计数（无重复桶）；静态面理由同 R46–R54 先例；无新配置项。

## Out of Scope

- 按 sessionId 分桶（会话标识敏感面——红线纪律）。
- 余弦分数分布直方图（地板语义既有，不改动）。
