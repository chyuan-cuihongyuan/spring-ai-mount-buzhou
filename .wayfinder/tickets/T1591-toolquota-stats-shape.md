---
id: T1591
title: 工具配额消耗读面（ToolQuotaStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1587
created: 2026-09-15
---

## Question

J 会话第 68 轮：guard/hook 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：ToolQuotaHook（per-tool 会话配额，spec 185 / T557）的 beforeTool 全路径只有 per-tool micrometer blocked 遥测——**配额消耗量（allowed）与未管辖豁免量无进程内直读面**。云厂商 per-API quota 消耗/拒绝对账思想（域内自源思想的静态面延伸，与 R57/R67 互补裁决同型）。

形状裁决：`ToolQuotaHook` 内静态 `AtomicLong` 五计数——calls（beforeTool 入口）/ allowed（放行即计，被拒不重复计——沿用既有语义）/ quotaBlocks（超限拒绝）/ unmanagedSkips（null ctx/toolName 与未配置限制合并——「不在配额管辖内」口径）+ 五计数第四桶 excludedTokens（坏状态值解析归零重计——NumberFormatException 静默修正显形）；嵌套 `record ToolQuotaStats` + `stats()` + `resetForTest()`。守恒 `calls = allowed + quotaBlocks + unmanagedSkips`（每入口恰落一桶）。hook 返回与改写语义逐位不变；micrometer 遥测原样保留。

Out of scope：按 tool 名分桶（既有 micrometer tag 已覆盖）；配额余量快照（会话态即真相源）。
