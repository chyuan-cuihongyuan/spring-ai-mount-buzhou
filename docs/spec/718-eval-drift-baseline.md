# 718 — 评估通过率漂移基线

> 来源：G 会话第 19 轮 = effort #718（Evidently drift 思想；EvalRunner 执行策略族补完）/ [T1036](../../.wayfinder/tickets/T1036-eval-drift-baseline.md) / [T1037](../../.wayfinder/tickets/T1037-eval-drift-baseline-verify.md) / impl 618。

## Problem

评估连跑的通过率突变（95% → 60%）是「数据集坏了/judge 坏了/模型坏了」的第一信号——但每次 run 都独立落盘，没人看趋势：漂移只能靠人翻历史 run 记录对比。EvalRunDiff 是 run 对 run 的明细 diff；**跨 run 的通过率基线**不存在。

## Solution

Evidently drift detection 思想（≈10K star：指标分布突变告警）：

- **opt-in**：`setDriftBaseline(int window, double warnShift)`——window = 基线取样数（同数据集最近 N 次 run），warnShift = 绝对漂移告警线 ∈ (0,1]；未设 = 关（默认零行为）。
- run 完成后：从既有 run 记录落盘（`eval.run.*`，零新存储）取同数据集、早于本次 startedAt 的最近 window 次 passRate 均值为基线；
- `|passRate − baseline| ≥ warnShift` → WARN（带基线/当前/差值三数）+ `buzhou.eval.drift.alerts` 计数；
- `lastDriftDelta()` 读数（最近一次 run 的当前−基线；无基线时 NaN）；
- 无历史样本（该数据集首跑）→ 跳过判定不告警。

## User Stories

1. 夜间评估：通过率一夜之间从 95% 掉到 60%——WARN 带差值，早班第一眼看到「评估体系异常」而非逐条查。
2. 阈值自调：window/warnShift 可调（保守 5 次 0.05 / 激进 2 次 0.15）。

## Implementation Decisions

- 基线用「早于本次 startedAt 的最近 window 次」——不含本次（防自污染）；同 startedAt 冲突按 runId 二次排序。
- 复用 run 记录落盘（spec 52 §D 形态，passRate 已在 JSON）——零新存储、零迁移。
- 只告警不阻断：漂移是信号不是错误，run 照常落盘（fsck 族「findings 不自动处置」同纪律）。

## Testing Decisions

- 植入 3 次历史 run（passRate=1.0）→ 当前 run passRate=0 → delta=−1.0、告警计数 1、lastDriftDelta=−1.0。
- window=2 → 只取最近 2 次为基线。
- 首跑（无历史）→ 无告警；默认关 → 任何漂移零告警。

## Out of Scope

- item 级分数分布漂移（detail 解析族后续轮）。
- 自动暂停评估调度（信号面不裁决策）。

## Further Notes

EvalRunner 执行策略族：预算（成本上限）/重试（抖动）/超时（挂死）/记忆化（复用）/漂移（趋势）——五件套收口。
