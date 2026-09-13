---
id: T1520
title: token 估算调用量与总量读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1519
created: 2026-09-14
---

## Question

J 会话第 34 轮：估算总量读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（TokenEstimateStatsTest，AssertJ 同仓风格）：estimate("abcd") → estimateCalls=1、totalEstimatedTokens=1（4 字符/token）；estimateMessages 批量按条累加；null 文本估 0 仍计调用；resetForTest 归零；实例隔离语义（静态则跨实例共享——单例口径入档）。定向 `mvn -pl buzhou-core test -Dtest='TokenEstimateStatsTest'` 绿 + 既有 TokenEstimator 回归绿。
