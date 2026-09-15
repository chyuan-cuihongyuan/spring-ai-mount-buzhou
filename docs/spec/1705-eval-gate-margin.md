# Spec 1705 — 门限边际直方读面（effort #1705，R6）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2611–T2612，impl 1305）。借鉴：
> Google SRE 告警边际 / SPRT 边际——「过 0.003」与「过 0.2」同是过，含金量
> 天壤；边际分布回答门判定的置信底座有多薄。

## Problem Statement

EvalGate 只吐过/不过：连续贴线通过（边际 0.01）的门分数被当稳态对待——
一次小回归就翻车；「这批 run 的门过得有多悬」无处可问。

## Solution

`EvalGateMargin`（core/eval，静态纯函数）：

- `analyze(passRates, threshold)` → `MarginReport(runs, threshold, margins,
  minMargin, maxMargin)`：margins = |rate − threshold|（入参序）；
- `MarginReport.withinBand(band)` → 危险带内 run 数（边际 ≤ band）——
  「多悬」直接读数；
- 空表哨兵：min/max = −1；null 按空表。

## User Stories

1. 作为评测维护者，minMargin=0.008 显形贴线通过——放行但记录在案。
2. 作为评测维护者，withinBand(0.02)=5/20——四分之一 run 在悬崖边，收紧门限前先稳分数。
3. 作为框架宿主，任意门限与通过率序列零适配入口。

## Implementation Decisions

- 纯读面不改 EvalGate 判定；两侧对称（|rate−threshold|——过线与差线同权）。
- 带宽由调用方声明（不同门的危险带不同）。

## Testing Decisions

- 边际逐项/最小/最大正确；带内计数三档（0/部分/全部）；
- 空表哨兵 −1；threshold 两侧（高于/低于）同权；
- null 按空表。

## Out of Scope

- 不改门判定逻辑/不做自动放行策略；不做分位（归 TurnLatencyPercentiles 族）。

## Further Notes

- 评测可观测五轴收齐：门（过/不过）→ 趋势（1444）→ 离散（1700）→
  覆盖（1702）→ 边际（本轮）+ 消序（1701）/指纹（1704）/位置偏差（1703）。
