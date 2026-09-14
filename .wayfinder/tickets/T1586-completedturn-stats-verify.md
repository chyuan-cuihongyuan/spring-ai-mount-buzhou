---
id: T1586
title: 完成轮检测器读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1585
created: 2026-09-15
---

## Question

J 会话第 65 轮：CompletedTurnStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（CompletedTurnStatsTest，BuzhouMessage 列表骨架——骨架见既有 MicroCompactor 测试）：含完成轮历史 → spansDetected=1 toolCallTurnsSeen≥1；全悬挂轮历史 → spansDetected=0 toolCallTurnsSeen≥1（失能信号可见）；resetForTest 归零。定向 `mvn -pl buzhou-memory -am test -Dtest='CompletedTurnStatsTest'` 绿 + 既有 CompletedTurnDetector 回归绿。
