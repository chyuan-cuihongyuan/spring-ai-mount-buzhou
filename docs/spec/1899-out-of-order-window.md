# Spec 1899 — 乱序接纳窗（effort #1899，R100）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2999–T3000，impl 1500）。借鉴：
> QuestDB（15K+ 星）out-of-order 接纳窗——时间线只允许「最新水位 −
> 窗宽」之后的迟到数据就地插入；窗前数据拒收（历史已定稿），窗后
> 追加。窗口宽窄是延迟与乱序容忍的换挡。

## Problem Statement

事件时间数据落库两难：严格按序要求全局重排（不可行），完全放任
乱序让历史定稿被反复改写——「多老的迟到数据不再收」的窗口判定
缺独立计算面。

## Solution

`OutOfOrderWindow`（core/metrics，静态纯函数 + Accept 枚举）：

- `classify(eventTimeMillis, highWaterMillis, windowMillis)`：三态——
  eventTime ≥ highWater → FRESH（顺序到达）；≥ highWater − 窗宽 →
  LATE_ACCEPTED（窗内迟到就地接纳）；否则 → TOO_OLD（窗前拒收）；
- `advance(highWaterMillis, eventTimeMillis)`：水位推进（取大，不
  回退）。

## User Stories

1. 作为时序落库者，水位 1000 窗 200：事件 900 → LATE_ACCEPTED
   就地插入；事件 700 → TOO_OLD 拒收——历史定稿有界。
2. 作为延迟调参者，窗宽即「乱序容忍换挡」——收宽窗 = 保旧数据，
   收窄窗 = 快定稿。
3. 作为确定性评审者，三态边界含下（恰在窗沿 = LATE_ACCEPTED）。

## Implementation Decisions

- 纯函数零状态；window ≥ 0、时点 ≥ 0 fail-fast；水位推进单调
  （取大）。

## Testing Decisions

- 三态各一例；窗沿边界两例（恰 FRESH 沿/恰 LATE 沿）；水位推进
  取大与持平；畸形两型（负窗/负时点）fail-fast。

## Out of Scope

- 不做真实插入与重排（归存储层）；不做多时间线水位。

## Further Notes

- 与 SequenceOrder（#recovery 序判定）互补：那是顺序对错，这是
  迟到接纳策略。
