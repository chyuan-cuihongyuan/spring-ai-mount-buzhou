# Spec 1822 — 混沌预算门（effort #1822，R23）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2845–T2846，impl 1423）。借鉴：
> Netflix Chaos Monkey / Chaos Toolkit——混沌实验的价值依赖有界爆炸半径：
> 只在允许窗口内且预算未烧完时放行，窗口约束优先于预算（安全第一）。

## Problem Statement

ChaosMonkeyHook 执行注入但没有**实验节律闸**：什么时候允许注入（低峰窗）、
本周期还允许注入多少（预算），缺裁决面——无界混沌等于自己 DDoS 自己。

## Solution

`ChaosBudgetGate`（core/exec，静态纯函数）：

- `Window(start, end)` 允许窗口（契约 start ≤ end；空窗口合法=永不放行）；
- `decide(budgetRemaining, now, window)` → 三态 `MAY_RUN / OUT_OF_BUDGET /
  FORBIDDEN_WINDOW`：**窗口优先于预算**（不在窗即禁，预算再多不跑）；窗内
  零预算即耗尽；
- `usage(budget, spentList)` → `Usage(spent, remaining(钳 0), burnRatio
  (无预算 -1 哨兵), exhausted())`：超支照实入账不外泄负值。

## User Stories

1. 作为混沌实验主持者，低峰窗 [02:00, 04:00] + 周预算 60 分钟 → 白天
   FORBIDDEN_WINDOW、夜里 MAY_RUN、烧完 OUT_OF_BUDGET——爆炸半径有界。
2. 作为值班者，burnRatio=1.2 → 本周期超支 20%，下周期该收紧。
3. 作为框架宿主，预算口径（分钟/次数）自声明，纯裁决零状态。

## Implementation Decisions

- 纯裁决不执行（注入归宿主，与 ChaosMonkeyHook 互补：那是执行器，这是
  节律闸）；窗口边界含两端。
- fail-fast：负预算、倒挂窗口、null/负实验花费；null 列表按空表。

## Testing Decisions

- 窗口优先三态；边界含两端+空窗口；使用账累计/钳零/烧尽比/超支 1.2；
  畸形四型 fail-fast。

## Out of Scope

- 不执行注入；不做实验选择（目标挑选归 ChaosMonkeyHook 既有策略）。

## Further Notes

- 与 ChaosMonkeyHook（执行器）构成「闸+执行」对；预算续期归宿主周期任务。
