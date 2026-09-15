---
id: T2622
title: 会话事件时间间隙检测的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2621
created: 2026-09-15
---

## Question

EventGapDetector 怎么验证？（spec 1710 验收/裁决）

## Resolution

EventGapDetectorTest：无间隙/两间隙+最大/恰等不计/<2 与 null 哨兵。
