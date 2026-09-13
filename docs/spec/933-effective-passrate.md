# 933 — 剪枝 run 有效通过率口径

> 来源：I 会话第 33 轮 = effort #933（[T1305](../../.wayfinder/tickets/T1305-effective-passrate-shape.md) / [T1306](../../.wayfinder/tickets/T1306-effective-passrate-verify.md) / impl 685）。spec 901 剪枝的口径收口（双口径显式并存）。

## 背景

spec 901 剪枝后 `total` 含 pruned 项——既有 `passRate = passed / total` 被系统性稀释（2 pass / 5 total（3 pruned）显 0.4，有效口径 2/2 = 1.0）。两口径各有用途：总量口径防「剪枝刷分」（CI 门语义从严），有效口径反映真实评估质量。

## 目标

- `EvalRunResult` 派生方法（record 加方法零破坏）：
  - `prunedCount()`：items 中 pruned 状态计数；
  - `effectivePassRate()`：`passed / (total − prunedCount)`；全 pruned（分母 0）约定 0.0（空集纪律一致）；
- 既有 `passRate()` 原样保留（总量口径——CI 硬门防剪枝刷分语义，javadoc 补记双口径辨义）。

## 兼容性

纯增量：record 新增派生方法，零既有行为变化。
