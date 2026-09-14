---
id: T1565
title: 情景记忆读写双守恒读面（EpisodicMemoryStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1563
created: 2026-09-15
---

## Question

J 会话第 55 轮：memory/episodic 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题（跨模块轮换入 memory 域）：EpisodeLedger 全路径零计数——record 静默丢弃（provider 缺失/goal 空白）、持久化异常吞掉、召回空结果与 few-shot 无命中全部不可见；「情景记忆是否在被写入、召回是否命中」无从回答。mem0 episodic memory 命中率思想。

形状裁决：`EpisodeLedger` 内静态 `AtomicLong` 九计数双守恒——写侧 recordCalls/recorded/recordDropped（provider 缺失或 goal 空白）/recordFailures（持久化异常），守恒 `recordCalls = recorded + recordDropped + recordFailures`；读侧 recallCalls/recallHits/recallEmpties/recallDropped（provider 缺失或 goal 空白），守恒 `recallCalls = recallHits + recallEmpties + recallDropped`（fewShotBlock 复用 recall 计数另加 recallCalls 单点，无重复桶）；嵌套 `record EpisodicMemoryStats` + `stats()` + `resetForTest()`。静态面理由同族先例；所有方法返回语义逐位不变。

Out of scope：按 sessionId 分桶（会话标识敏感面）；余弦分数分布直方图（地板 0.10 既有语义不变）。
