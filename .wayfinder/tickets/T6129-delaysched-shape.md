---
id: T6129
title: S 会话 S15 延迟调度的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

数据本地性等待怎么不饿死也不浪费带宽？（spec 5014 /
effort #5014 / S15）

## Resolution

**DelayScheduling（core/policy）**：Spark locality wait 思想
——首选可用零浪费即启；不可用 WAIT 累计 skips，超 maxSkips
预算 → LAUNCH(ANY) 并重置（本地性换时效）；降级梯逐轮放宽；
确定性无时间依赖。
