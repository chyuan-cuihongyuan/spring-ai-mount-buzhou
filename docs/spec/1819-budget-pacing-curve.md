# Spec 1819 — 预算花费匀速曲线（effort #1819，R20）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2839–T2840，impl 1420）。借鉴：
> 广告投放 spend pacing——预算周期内匀速是健康基线，偏离分三态，
> run-rate 回答期末烧到几倍。

## Problem Statement`

预算（token/费用）只看「还剩多少」不看「花得多快」：前半周期烧掉 80%
的预算在余量读数里要到快断粮才显形；匀速基线（target = 预算 × 已过时间比）
与偏离三态无处判。

## Solution

`BudgetPacingCurve`（core/budget，静态纯函数）：

- `evaluate(elapsedFraction, spentFraction, tolerance)` → `PacingReport(
  elapsedFraction, spentFraction, targetFraction, pacing)`：三态 ON_PACE
  （|偏离| ≤ 容差，边界含且浮点噪声免疫）/ OVER_PACING（超前烧钱）/ 
  UNDER_PACING（落后漏损）；
- `deviation()` 偏离（正=超前）+ `runRate()` 运行率 = spent/elapsed
  （elapsed=0 时 -1 哨兵；期末=1 恰好匀速）。

## User Stories

1. 作为预算持有者，elapsed=0.3/spent=0.6 → OVER_PACING + runRate=2.0——
   按当前速度期末烧两倍，现在就该收。
2. 作为周期治理者，UNDER_PACING 常态 = 预算申报虚高，下周期该调小。
3. 作为框架宿主，周期口径（日/周/月）自声明，纯判态零干预。

## Implementation Decisions

- 纯判态不干预（限流归宿主）；边界含 + FLOAT_EPSILON=1e-12 浮点噪声免疫
 （0.55−0.5 二进制尾差不翻态——L 系 impl1305 假红病理的实现侧根治）。
- fail-fast：分数越界/NaN、负容差。

## Testing Decisions

- 三态+偏离+运行率；边界含（0.55/0.5/0.05 噪声对不翻态）；周期首尾端点；
  畸形四型 fail-fast。首跑双红即浮点病理实证，EPSILON 修正后绿。

## Out of Scope

- 不做自动限速；不做分段非线性基线（前端加载式归未来静脉）。

## Further Notes

- 与 CostForecast/ElasticBudgetPool 正交：那是预测与弹性池，这是节奏判态。
