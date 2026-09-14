---
id: T2431
title: R41 PII 豁免计数的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2430
created: 2026-09-15
---

## Question

N 会话第 41 轮：豁免计数分侧（输出/输入）还是合并？

## Resolution

选 **合并口径**。对照面问题（「豁免面是否过宽」）不需要分侧精度；
PiiHitStats 已有 Side 维度给命中——豁免加 Side 是过度设计，exemptionsApplied
单一计数与总命中对照即可。
