---
id: T6157
title: S 会话 S29 Segment Log 分段日志的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

追加日志怎么容量滚动且保留上限防耗尽？（spec 5028 /
effort #5028 / S29）

## Resolution

**SegmentLog（core/recovery）**：Kafka 分段日志思想——append
递增 LSN、段满滚动新段、段数超 maxSegments 淘汰最旧段
（droppedCount 诚实可见）；readAll 跨段按序；畸形 fail-fast。
