---
id: T2407
title: R29 熔断慢调用维度的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2406
created: 2026-09-15
---

## Question

N 会话第 29 轮：慢调用维度扩 Config 还是链式注入？

## Resolution

选 **链式注入（withSlowCallPolicy）**。Config（Circuit record）已 10 参 + 4
兼容构造——再扩两参破坏面大；链式注入零配置零行为（NaN/未注入=维度关），
装配侧（ResilienceModule）后续按需接 yml。慢样本独立环形窗（懒建）而非
复用失败窗——两维度语义独立可同时生效。
