# 710 — 全局 holdout 层

> 来源：G 会话第 11 轮 = effort #710（505/709 实验族的跨实验控制组层）/ [T1020](../../.wayfinder/tickets/T1020-experiment-holdout.md) / [T1021](../../.wayfinder/tickets/T1021-experiment-holdout-verify.md) / impl 610。

## Problem

所有用户都在某个实验里时，「实验总体有没有正效应」无法回答——没有不被任何实验触碰的对照组。Statsig/LaunchDarkly 的 holdout layer 语义：固定比例 unit 全实验排除，其行为与实验内用户对比 = 实验组合的净效应。

## Solution

- 构造器再扩：`ExperimentBucketer(experiments, expiresAt, holdoutPercent, clock)`（原两参构造委托 holdout=0 零变化）。
- **判定**：assign() 在未知实验/到期检查后、变体落桶前：`floorMod100("holdout", unitKey) < holdoutPercent` → 返回 null + 曝光计该实验 `__holdout__` 独立桶 + `buzhou.experiment.holdout` 计数。
- **层语义**：holdout 哈希只含 unitKey 不含实验名——同 unit 在**所有实验**一致被排除（与 505 的「哈希含实验名独立随机」刻意相反——层要的就是全局一致）。
- 读数：`holdoutPercent()`。

## User Stories

1. 平台团队：设 5% holdout 跑季度——期末对比 holdout 组与实验组的总体指标，回答「这季度实验整体赚了还是亏了」。

## Implementation Decisions

- holdout 判定放到期**之后**：过期实验的 `__expired__` 口径不被 holdout 抢写（单桶归属唯一）。
- 静态配置（构造期定死）——运行期动态调 holdout 比例会污染对照纯度，刻意不做。

## Testing Decisions

- holdout=100 → 所有 unit 全实验 null+__holdout__ 计数；holdout=0 → 与既有行为逐字节一致；holdout=50 → 同 unit 跨两实验一致排除（层语义）。

## Out of Scope

- 动态调比例（污染对照——刻意排除）。
- 分层实验（layer→多个互斥实验组）——族后续轮。
