# Spec 1879 — 协同遗漏校正审计（effort #1879，R80）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2959–T2960，impl 1480）。借鉴：
> HdrHistogram/jHiccup（Gil Tene）的 Coordinated Omission 校正——固定
> 速率压测中一个慢响应会「协同」推迟后续发送，停顿期的真实体验从
> 原始记录里整体消失；期望间隔阶梯补账把盲区找回来。

## Problem Statement

压测/调用的延迟记录只记「实际发出且返回」的请求：一个 450ms 的慢
响应把本该 100ms 一发的后续请求整体推迟——停顿期本会观察到的多次
延迟样本从未存在，原始分位数系统性偏乐观（协同遗漏），且没有账面
暴露这个盲区。

## Solution

`CoordinatedOmissionAudit`（core/metrics，静态纯函数，HdrHistogram
expectedInterval 阶梯语义）：

- `correctedSampleCount(observedLatency, expectedInterval)`：该慢响应
  应补记的样本数（观测 1 + 阶梯补记；latency ≤ interval 即 1——
  无遗漏；450/100 → 450,350,250,150 = 4）；
- `omittedCount(observedLatency, expectedInterval)`：补记数 −1——
  本次慢响应掩盖了多少次发送机会；
- `blindWindow(observedLatency, expectedInterval)`：latency − interval
  与 0 取大——协同静默窗长；
- `coverageRatio(observedSamples, correctedSamples)`：observed/corrected
  ——原始记录覆盖真实需求的比例（1.0 无盲区）。

## User Stories

1. 作为压测作者，450ms 慢响应在 100ms 间隔下补 3 样本——停顿期
   「其实每次都要等」进入分位数。
2. 作为报告评审者，coverageRatio=0.25 → 原始数据只看到四分之一
   的真实需求——报告可信度有账。
3. 作为框架宿主，纯计算不计时序——校正口径由调用方声明。

## Implementation Decisions

- 阶梯只计数不物化（HdrHistogram 语义的计数面）；interval ≥ 1、
  时延 ≥ 0、correctedSamples ≥ 1 fail-fast；coverage 校正数 < 观测数
  即畸形（补账不可能减样本）。

## Testing Decisions

- 阶梯四例（450→4/100→1/50→1/0→1）；遗漏与盲窗三例（3/0/0 与
  350/0/0）；覆盖率两例（10/40=0.25、40/40=1.0）；畸形四型
  fail-fast。

## Out of Scope

- 不做延迟分布直方（归 LogBucketHistogram/MedianKeeper 面）；不
  自动改写观测记录（校正口径归调用方）。

## Further Notes

- 与 MisraGriesSketch（频次素描）互补：那是谁多，这是漏了多少。
