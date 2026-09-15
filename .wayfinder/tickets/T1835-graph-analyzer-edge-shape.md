---
id: T1835
title: R14 选题——ToolGraphAnalyzer 边缘分支（cycles null fail-fast/零计数边/tie-break/null kind/startedAt）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 14 轮：批次 4 收尾——ToolGraphAnalyzer（11 missed）既有测试已密（edges/cycles/flame 三套），残余 1–3 missed/方法的边缘分支如何精确定位补测？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 14 轮 = effort #1213 / spec 1213 / impl 916）：

1. **补测面（既有三套测试外的边缘分支，jacoco 行号精确定位）**：cycles(null) fail-fast（未测）；count≤0 边不入邻接（零计数边不成环）；边 count 同值时 from→to 字典序 tie-break；analyze 的 kind=null span 忽略（既有测试只测非 TOOL 值，未测 null 值）；timings 的 startedAt=null 计 0 与 null spanId 不入父子索引（跳 memo/visiting）；负时长夹 0。
2. **形态**：ToolGraphAnalyzerEdgeTest 单文件 6 用例（新增边缘测试类，不动既有三套）。
3. **边界**：不改主代码；ObservabilityAdvisor 剩余 96 missed（流式 harness 已落，细粒度 captureInjectionSnapshot 等）视 R15 复扫余量决定。
