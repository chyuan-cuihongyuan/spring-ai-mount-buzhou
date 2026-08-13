---
id: T17
title: core · 有界 Turn + 可组合停止条件（杜绝 runaway 循环）
type: task
status: closed
assignee: "chyuan"
blocked-by: []
epic: T12
created: 2026-08-13
---

**Status:** ready-for-agent · **Spec:** [docs/spec/11](../../docs/spec/11-best-of-breed-adoption.md#core)

## What to build

为 Turn 循环引入可配置的 think→tool 递归上界（默认保守值，可配），超界走可插拔兜底 handler 产出优雅最终回复。停止条件建模为**可组合 `Predicate<TurnContext>` 链**（预算 | 超时 | 工具信号 | 外部取消，支持与/或）。外部行为：模型陷入工具死循环时，Turn 在预算内终止并优雅收尾。

## Acceptance criteria

- [ ] 端到端：模型无限循环调用工具时，Turn 在预算内终止、产出优雅最终回复（非崩溃/非无限烧 token）
- [ ] 停止条件可组合（至少预算 + 超时两条可用与/或组合）
- [ ] 正常单轮/多轮 Turn 行为不回归

## Blocked by

无 —— 可立即开工。（与 T16 同在执行脊柱，建议串行以免冲突。）

## Resolution

已落地（Tier-1）。`TurnLoopPolicy`/`TurnLoopContext`（core/session）+ `BoundedToolCallingAdvisor`（继承 Spring AI `ToolCallingAdvisor`，挂「模型响应后、工具执行前」缝隙）：命中即把工具调用响应替换为优雅最终回复（可插兜底 handler），循环自然退出。停止条件 = 可组合 `Predicate<TurnLoopContext>`（JDK and/or，内置轮数预算 + 超时；工具信号/外部取消由接入方 Predicate 表达）。默认 40 轮保守上界、`unbounded()` 逃生舱；经 `RuntimeConfig` 第 9 组件 `turnLoopPolicy` 注入。事件 `turn.loop.bounded` 可观测。spec 05 新增专节。测试：`BoundedTurnLoopTest`（6：死循环预算内收尾/自定义兜底/超时/组合语义/正常轮不回归）、examples `BoundedTurnIntegrationTest`。
