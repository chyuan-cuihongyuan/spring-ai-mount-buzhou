---
id: T1585
title: 完成轮检测器读面（CompletedTurnStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1583
created: 2026-09-15
---

## Question

J 会话第 65 轮：memory/compact 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：DefaultCompletedTurnDetector.detectTurns（完成轮检测——微压缩触发的前提判定）全路径零计数：检出 0 时压缩永远不触发这一系统性故障不可见（历史全含未闭合工具调用的「悬挂轮」时静默失能）。检测器是压缩管线的上游闸门（OpenTelemetry span 完成判定思想——上游闸门的空结果率是管线失能的第一信号）。

形状裁决：`DefaultCompletedTurnDetector` 内静态 `AtomicLong` 三计数——detectCalls（入口）/ spansDetected（检出完成轮累计）/ toolCallTurnsSeen（含工具调用的 ASSISTANT 消息扫过数——有潜在可检出量的分母）；嵌套 `record CompletedTurnStats` + `stats()` + `resetForTest()`。口径诚实：spansDetected ≤ toolCallTurnsSeen 为弱校验（非硬守恒——同一轮可被多次扫描），检出率 = spansDetected/toolCallTurnsSeen 为健康信号。detectTurns 返回语义逐位不变。

Out of scope：按 sessionId/历史长度分桶（敏感面红线）；悬挂轮成因分类（语义归上游回调缺失，另轴）。
