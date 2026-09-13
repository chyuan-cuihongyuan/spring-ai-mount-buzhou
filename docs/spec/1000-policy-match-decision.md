# 1000 — 工具策略匹配决策读面

> 来源：J 会话第 1 轮 = effort #1000（[T1451](../../.wayfinder/tickets/T1451-policy-match-decision-shape.md) / [T1452](../../.wayfinder/tickets/T1452-policy-match-decision-verify.md) / impl 753）。借鉴：Open Policy Agent [decision log](https://www.openpolicyagent.org/docs/management-decision-logs)——「每次判定留痕：谁、按哪条规则、判成什么」。

## Problem Statement

工具级策略四层覆盖模型（spec 02）经 `ToolPolicyMatcher.match` 解析：精确名优先、通配最长前缀胜出、未命中返回空 Map。但匹配过程零观测——「这个工具的策略是精确配的还是通配扫出来的」「有多少调用压根没配策略」不可见；策略配置退化（该精确配的都落在 `*` 兜底上）无信号。

## 目标

- 新公共值类型 `ToolPolicyMatchDecision`（core.policy，api 面）：`record(toolName, Outcome, matchedKey)`；嵌套枚举 `Outcome = EXACT | GLOB | NONE`（EXACT→matchedKey=工具名本身；GLOB→胜出的通配模式；NONE→空串）。
- 新公共快照 `ToolPolicyMatchStats(exactHits, globHits, noneHits, recent)`：守恒不变量 Σ三分类 == 自上次 reset 以来 match 调用总数（同一计数单点）；`recent` 有界环形（32 条，新→旧）。
- `ToolPolicyMatcher.match` 判定单点同步累加（返回值语义逐位不变——现有调用方零感知）；`stats()` 读面 + `resetStats()` 测试隔离注入点（进程级静态态，BuzhouMetricsHolder 先例）。

## 兼容性

纯增量读面：match 返回值不变、无新配置项、默认启用（纯内存计数，每工具调用一次的低频路径，单锁无争用担忧）。

## Out of Scope

- RunawayHook 中复刻的同口径 glob 算法去重（独立重构，不混读面轮）。
- 决策持久化/导出（OPA decision log 有存储后端；本仓读面止步进程内快照）。
