# 1413 — 结构化输出 REASK 读数

> 来源：L 会话第 14 轮 = effort #1413（票 T2127 / T2128 / impl 1066）。借鉴：Instructor（max_retries + REASK 的可观测面——重试漏斗合规率显形；机制本体 spec 19/T87 已建，本轴补读数）。

## Problem Statement

`chatForEntity`（spec 19 结构化输出：首解析→`structured.reask`→再解析→或 `StructureduredOutputException`）的漏斗全程无读面：模型 JSON 合规率、REASK 触发频度、最终失败率全靠感觉。「这个模型适不适合结构化输出」无数据可断。

## 目标

- `StructuredOutputStats`（core/session，进程级静态读面，ToolArgsValidator.validationStats 同款先例——埋点在 internal 主路径、读面归公共类）：
  - 漏斗五计数：`attempts`（chatForEntity 首解析起点）/ `firstPassParsed`（首轮合规）/ `reasks`（REASK 触发）/ `reaskParsed`（REASK 后合规）/ `failures`（REASK 后仍败）；
  - **双守恒式**：`attempts = firstPassParsed + reaskParsed + failures`、`reasks = reaskParsed + failures`（REASK 恰一次——既有语义）；
  - 派生 `firstPassRate()`（模型 JSON 首过合规率；attempts=0 哨兵 -1）；
  - `stats()` 嵌套 `record Snapshot` + `resetForTest()` 归零口。
- 埋点：DefaultAgentSession.chatForEntity 五点（行为逐位不变——只增记账）。

## 兼容性

纯增量读面：REASK 语义/异常路径/预算计入逐位不变；静态面理由同 R11（validate 静态入口全覆盖）；record 方法 public（跨包埋点）但语义为内部埋点（类注如实声明）。

## Out of Scope

- 按类型（Class）分桶（基数失控红线）。
- REASK 次数配置化（max_retries>1——机制变更另轮）。
- PairwiseEvalRunner 等其他解析点的读面延伸（只覆盖 chatForEntity 漏斗，口径显式）。
