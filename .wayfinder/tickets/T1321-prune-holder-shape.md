---
id: T1321
title: 评估剪枝进程级兜底装配的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 57 轮：spec 901 剪枝的声明式装配（`new EvalRunner` 非 bean 路径）如何收口？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 57 轮 = effort #958 / spec 958 / impl 701）：`EvalPrunePolicyHolder`（进程级 AtomicReference，RetryBudgetHolder 同款）+ autoconfig `buzhou.eval.prune.{enabled,min-items,fail-rate-threshold}` 装配 bean（ConditionalOnProperty + DisposableBean 清理）；EvalRunner prune 解析实例优先、Holder 兜底。优先级：实例显式 > Holder 进程级 > 关闭。
