---
id: T2637
title: 钩子取消面统计的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

HookCancelStats 的形状怎么裁决？（spec 1718 / effort #1718 / R19）（spec 1718 验收/裁决）

## Resolution

实例面 record(boolean ran)+snapshot(observed/cancelledSkipped/completed/cancelRatio −1 哨兵)+resetForTest——OTel exporter 取消路径遥测，取消占比分辨「没跑因取消 vs 没注册」。
