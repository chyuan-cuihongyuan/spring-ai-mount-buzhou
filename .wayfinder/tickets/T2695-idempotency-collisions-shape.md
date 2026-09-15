---
id: T2695
title: 幂等键冲突读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

IdempotencyCollisions 的形状怎么裁决？（spec 1747 / effort #1747 / R48）（spec 1747 验收/裁决）

## Resolution

实例面 record(key, replayed)（null/空归 _blank_）+distinct 键集有界 256 FIFO 逐出+census(distinctKeys/totalRecords/replayedRecords/collisionRatio 无样本 −1)——Stripe 幂等键遥测，冲突占比异常=键生成缺陷。
