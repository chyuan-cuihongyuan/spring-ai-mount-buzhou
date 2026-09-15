# Spec 1856 — 利特尔法则一致性审计（effort #1856，R57）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2913–T2914，impl 1457）。借鉴：
> 排队论 Little's Law（L = λ × W）——稳态下并发/到达率/逗留时间必然
> 互证，偏差即仪表失真或稳态破。

## Problem Statement

三个常用指标（并发数、到达率、平均逗留）各自汇报、各自告警——但它们
在稳态下被排队论锁定互证：任两者定第三者。互证偏差无人查——仪表漂移
（计数漏/时钟偏）与稳态破（突发积压）都藏在各自「正常」的读数里。

## Solution

`LittlesLawAudit`（core/metrics，静态纯函数）：

- `impliedConcurrency(arrivalRatePerSec, sojournMillis)`：L = λ × W
 （毫秒→秒换算内置）；
- `consistency(measuredL, λ, W, toleranceRatio)` → `CONSISTENT /
  DIVERGENT`：|L − λW| ≤ tol × max(|λW|, 1)（零基线退化绝对口径——
  WindowShiftDetector 同惯例）。

## User Stories

1. 作为可观测作者，λ=10/s、W=200ms → 隐含并发必为 2——实测并发表读
   4 即 DIVERGENT：三处仪表至少一处错，或系统在积压。
2. 作为 SRE，互证审计常驻健康检查——单指标自证可信度低一个量级。
3. 作为框架宿主，指标口径自声明，纯审计不归因。

## Implementation Decisions

- 纯审计不归因（失真定位归宿主）；零基线分母 max(|λW|,1) 退化。

## Testing Decisions

- 隐含并发换算（10×200ms→2）；互证容差内/外；零基线退化；畸形四型
  fail-fast。

## Out of Scope

- 不做分布级检验（M/G/1 深化归未来静脉）；不接健康端点（接线归后续轮）。

## Further Notes

- 与 BuzhouHealth 互补：那是分项健康，这是跨指标互证。
