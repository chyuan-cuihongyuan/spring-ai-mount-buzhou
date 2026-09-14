---
id: T1600
title: PII 检测引擎读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1599
created: 2026-09-15
---

## Question

J 会话第 72 轮：PiiDetectorStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（PiiDetectorStatsTest）：含 PII 文本 scan → scansWithHits=1 matchesFound≥1；干净文本 scan → 弱校验（scanCalls 增 hits 不增）；pseudonymize → pseudonymizeCalls=1；resetForTest 归零。定向 `mvn -pl buzhou-guard -am test -Dtest='PiiDetectorStatsTest'` 绿 + 既有 PiiDetector 回归绿。
