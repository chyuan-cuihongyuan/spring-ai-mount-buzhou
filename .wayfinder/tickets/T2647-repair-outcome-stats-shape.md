---
id: T2647
title: 悬挂修复动作结果普查的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

RepairOutcomeStats 的形状怎么裁决？（spec 1723 / effort #1723 / R24）（spec 1723 验收/裁决）

## Resolution

Action 闭集 REPLAYED/MARKED_FAILED/SKIPPED_GONE+record+census(total/replayed/markedFailed/skippedGone/replayRatio −1 哨兵)+resetForTest——k8s events 自愈动作审计，与 DanglingTurnDetector 互补。
