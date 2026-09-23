---
id: T6158
title: S 会话 S29 Segment Log 分段日志的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6157]
created: 2026-09-24
---

## Question

S29 合同怎么逐一验绿？（spec 5028 / effort #5028 / S29）

## Resolution

**验证通过**：SegmentLogTest 四测全绿——段满滚动；超上限逐
最旧段（dropped 计数与 firstSurvivingLs n 读数）；readAll 跨
段按序；畸形参数/记录 fail-fast + 确定性回放。
