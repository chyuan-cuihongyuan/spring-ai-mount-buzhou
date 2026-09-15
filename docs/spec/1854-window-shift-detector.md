# Spec 1854 — 双窗口漂移检测（effort #1854，R55）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2909–T2910，impl 1455）。借鉴：
> Netflix/Google SRE 双窗口异常检测惯例（近期窗 vs 基线窗）——均值漂移
> 双闸显著：绝对闸（量级重要吗）× 相对闸（比例重要吗）。

## Problem Statement

指标告警的单闸二难：只看相对差被小基数噪声刷屏（基线 0.1 涨到 0.3 = 
+200% 但无关痛痒）、只看绝对差漏报缓变（大基数缓涨每个绝对差都不显著
但累积致命）——「重要且显著」的双闸漂移判定缺面。

## Solution

`WindowShiftDetector`（core/metrics，静态纯函数）：

- `detect(baseline, recent, minAbsoluteDelta, minEffectRatio)` → 三态
  `STABLE / SHIFTED_UP（变差）/ SHIFTED_DOWN（变好）`；
- 双闸同时过才漂：|recentMean − baselineMean| ≥ 绝对闸 **且** ≥ 相对闸 ×
  max(|baselineMean|, 1.0)（零基线退化绝对口径）。

## User Stories

1. 作为告警作者，延迟 100→150ms（绝对 50≥20 且相对 50%≥20%）双闸过 →
   SHIFTED_UP 变差告警；小基数 +200% 绝对不过 → 静默。
2. 作为优化验证者，SHIFTED_DOWN = 优化生效的读数证据。
3. 作为框架宿主，窗口口径自声明，纯判漂不归因。

## Implementation Decisions

- 纯判漂不归因（根因归宿主）；零基线分母 max(|基线|,1) 退化入档。

## Testing Decisions

- 双向漂移；双闸缺一不可（绝对过相对不过 STABLE）；零基线退化；畸形
  四型 fail-fast。

## Out of Scope

- 不做方差/分布检验（t-test/Wasserstein 归未来静脉）；不接告警通道。

## Further Notes

- 与 SloMultiWindowBurn 正交：那是 SLO 燃烧率，这是通用均值漂移双闸。
