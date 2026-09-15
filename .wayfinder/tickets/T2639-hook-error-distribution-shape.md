---
id: T2639
title: 钩子异常类型分布的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

HookErrorDistribution 的形状怎么裁决？（spec 1719 / effort #1719 / R20）（spec 1719 验收/裁决）

## Resolution

「钩子名:异常简单类名」指纹分组+基数有界默认 32 超出并 _overflow_ 桶+census() 降序保序（unmodifiableMap）+total——Sentry 分组思想；指纹不带消息（聚相似不聚文案）。
