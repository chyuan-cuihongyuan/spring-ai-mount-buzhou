# Spec 1921 — 时钟抖动测量（effort #1921，R122）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3043–T3044，impl 1522）。借鉴：
> NTP/RFC 5905 时钟 discipline 惯例——偏斜（skew，恒定差）与抖动
> （jitter，差的方差）是两个量：偏斜可校、抖动只可测。滑动窗口内
> 采样偏差的标准差 = 时钟源稳定性读数。

## Problem Statement

时钟源「稳不稳」没有量化口径：偏斜校正（R117）假设偏斜恒定，但
虚拟机时钟会漂移抖动——抖动多大时钳位校正不可信、该告警换源，
缺独立测量面。

## Solution

`ClockJitterMeter`（core/metrics，持态小 keeper）：

- `record(offsetMillis)`：记录一次本地-参考时钟偏差采样（可为负
  ——时钟可快可慢）；
- `jitterMillis()`：窗口内偏差总体标准差——稳定性读数（样本 < 2
  哨兵 -1.0 诚实「样本不足」）；
- `meanOffsetMillis()`：窗口平均偏差（偏斜分量读数）；
- 窗口定长滚动（满则覆盖最旧）。

## User Stories

1. 作为时钟监护者，偏差恒定 50ms → 抖动 0——纯偏斜可校。
2. 作为告警作者，抖动 > 5ms → 时钟源不稳告警——钳位校正可信度
   前置判定。
3. 作为排障者，mean 偏差 + jitter 分开读——恒定差与漂移分开归因。

## Implementation Decisions

- 定长滚动窗口（默认 16 采样）；样本 < 2 哨兵 -1.0；窗口大小 ≥ 2
  fail-fast；offset 可负（可变 long）。

## Testing Decisions

- 恒定偏斜抖动 0；已知样本集抖动精确；均值符号正确；不足样本
  哨兵；窗口滚动覆盖；畸形（窗口 < 2）fail-fast。

## Out of Scope

- 不做时钟同步（归 NTP）；不做偏斜校正执行（归 ClockSkewClamp
  面）。

## Further Notes

- 与 ClockSkewClamp（R117）成对：那是偏斜后果的钳位，这是抖动
  本身的测量——「校正可信度」的前置仪表。
