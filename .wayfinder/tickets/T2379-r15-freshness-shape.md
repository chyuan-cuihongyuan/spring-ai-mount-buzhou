---
id: T2379
title: R15 指标新鲜度接线形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2378
created: 2026-09-15
---

## Question

N 会话第 15 轮：新鲜度装饰器恒包还是 opt-in？

## Resolution

选 **恒包**。有界 512 名 + 每写入一次 map put 的纯旁路成本——配置面负担大于
运行成本；且新鲜度的价值在「事后查为什么没数」——装了才有基线，opt-in 会在
最需要时缺席。audit 经 Holder 静态面暴露（端点段后续轮）。
