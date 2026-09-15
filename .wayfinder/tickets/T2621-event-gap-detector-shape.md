---
id: T2621
title: 会话事件时间间隙检测的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

EventGapDetector 的形状怎么裁决？（spec 1710 / effort #1710 / R11）（spec 1710 验收/裁决）

## Resolution

静态纯函数 analyze(eventEpochMillis, thresholdMillis)→GapReport(events/threshold/gapCount/largestGapMillis)；间隙=相邻差>阈值（严格大于）；<2 事件哨兵 −1；largest 取绝对差容忍乱序——Flink event-time gap 思想，序维（TurnSequenceAudit）之外的时维。
