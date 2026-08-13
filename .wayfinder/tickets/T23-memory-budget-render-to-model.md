---
id: T23
title: memory · 动态预算渲染给模型（每段 chars_current/limit）
type: task
status: closed
assignee: "chyuan"
blocked-by: []
epic: T13
created: 2026-08-13
---

**Status:** ready-for-agent · **Spec:** [docs/spec/11](../../docs/spec/11-best-of-breed-adoption.md#memory)

## What to build

注入视图在九段式摘要每段末尾渲染 `chars_current / chars_limit` 页脚，让模型感知预算压力、主动削低优先级（P3）段。把 Buzhou 既有的动态预算**暴露给模型**，而非仅内部计算。

## Acceptance criteria

- [ ] 渲染后的 prompt 含每段 `chars_current/limit` 页脚
- [ ] 预算压力下模型可自削 P3（评测式断言：高压场景 P3 被裁而 P0 保留）
- [ ] 既有动态预算计算与注入视图不回归

## Blocked by

无 —— 可立即开工。

## Resolution

已落地（Tier-1）。`SegmentBudgetPlanner`（buzhou-memory/budget）：动态摘要 token 预算按 P0 40%/P1 30%/P2 20%/P3 10% 拆分为段字符预算，注入视图渲染时头部带预算提示（引导自削 P3）、九段每段末尾 `（本段 X/Y 字符）` 页脚、超限段带精简告警；`InjectionViewProcessor.assembleWithSummary` 接线（预算来自 `BudgetReport.historyBudget`）。spec 01 新增条目。测试：`SegmentBudgetPlannerTest`（3：页脚/超限告警/优先级份额）、examples `BudgetFooterInjectionTest`（会话接缝：注入视图含页脚 + P0 保真）。
