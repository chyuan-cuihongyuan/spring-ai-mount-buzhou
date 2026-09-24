---
id: T6178
title: S 会话 S39 Shuffle Sharding 洗牌分片的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6177]
created: 2026-09-25
---

## Question

S39 合同怎么逐一验绿？（spec 5038 / effort #5038 / S39）

## Resolution

**验证通过**：ShuffleShardingTest 五测全绿——圣像分配 4 租户
钉住；同种子同分配；routesTo 全域一致；200 租户平均重叠
0.0398<0.1（期望 0.04）+大小/去重；畸形 fail-fast（含
null 前移防 CHM NPE）。
