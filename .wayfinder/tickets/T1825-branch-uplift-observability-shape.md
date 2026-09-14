---
id: T1825
title: R9 分支缺口批次 2 选题与形态（observability：SessionState × ObservableToolCallback）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 9 轮：observability（BRANCH 66.7%，第二差模块）的逐类缺口（ObservabilityAdvisor 68 / MicrometerDualWriter 15 / ObservabilitySessionState 13 / ObservableToolCallback 10 等）如何选题与切批？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 9 轮 = effort #1208 / spec 1208 / impl 911）：

1. **本批 = 两小类先清**：ObservabilitySessionState（covered 11/missed 13）与 ObservableToolCallback（4/10）——两者共享 fake 基建（RecordingSpanRecorder + RecordingSpanHandle），小类先清可低成本把 fake 沉淀为批次 3（ObservabilityAdvisor 68 missed / MicrometerDualWriter 15）的先例。
2. **ObservabilitySessionState 补测面**：onTurnStart 无 session 早退、长输入 200 截断与 null 输入、usage 累加与 onTurnStart 重置、onTurnEnd/onTurnError 属性与关闭、二次 onTurnEnd 幂等、onClose/onCancel flush、carrier null 防御、currentTurnSeq/nextIteration。
3. **ObservableToolCallback 补测面**：成功路径四段（开 span→TOOL_INPUT→委托→TOOL_OUTPUT→close）、委托异常 error+close+rethrow、parent 解析三优先级（ToolContext 载体 > 字段载体 > hooks.sessionSpan 兜底）与 null 兜底、单参 call 委托、tool.parallel.index 由 carrier 计数。
4. **边界**：不改主代码；ObservabilityAdvisor/MicrometerDualWriter 归批次 3（R10 候选）。
