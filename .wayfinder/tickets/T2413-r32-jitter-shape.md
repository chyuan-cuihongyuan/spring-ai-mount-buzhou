---
id: T2413
title: R32 退避抖动模式的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2412
created: 2026-09-15
---

## Question

N 会话第 32 轮：jitter 模式改默认还是可配？

## Resolution

选 **可配默认 EQUAL**。改默认 FULL 是行为变更（既有调度的退避分布整体变化）
——0.x 语义允许但无必要：FULL 的收益场景（重试风暴）由运维按负载特征选择。
DECORRELATED 的 prev 状态会话内 volatile（advisor 每会话实例——语义域正确）。
