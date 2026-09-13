---
id: T1062
title: 评估分数分布解析的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

714 detail 分数留痕无消费面——加解析统计吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 32 轮 = effort #731 / spec 731 / impl 631）：`EvalScoreAnalytics.similarityScores(run)` 纯函数——正则提取 similarity= 分数→scored/min/max/mean/scores；无分数项跳过诚实计数；空集 NaN。
