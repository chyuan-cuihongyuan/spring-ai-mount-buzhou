---
id: U7253
title: U 会话 U27 TimeBucketReservoir 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

指标新鲜度怎么保？（spec 7026 / effort #7026 / U27）

## Resolution

**TimeBucketReservoir（core/metrics）**：时间戳入样+窗口快照+显式清理；水位单调 fail-fast；时间注入确定。
