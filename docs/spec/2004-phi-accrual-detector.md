# Spec 2004 — φ 累积故障嫌疑度检测器（effort #2004，R5）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3109–T3110，impl 1555）。
> 借鉴：Hayashibara φ-accrual / Finagle-Akka 故障检测——连续嫌疑度
> 取代二值 up/down。

## Problem Statement

现有健康判定是二值跳变：一到超时线即判死。心跳抖动的进程会被误杀
（瞬时 GC 停顿 = 失联？），真死进程的判定时机又无梯度可解释——监控
面板上只有「好/坏」没有「多可疑」。

## Solution

`PhiAccrualFailureDetector`（core/concurrent，synchronized 小临界区）：

- `heartbeat(nowMillis)`：间隔入滑动窗（默认 1000 样本；时间回拨
  fail-fast；首个心跳只锚定）；
- `phi(nowMillis)` = −log₁₀(P(此久未心跳 | 正常))：间隔正态模型
  （mean/std + std 下界钳 100ms 防规律心跳退化），右尾概率经 erf
  近似（Abramowitz-Stegun 7.1.26，|误差| ≤ 1.5e−7）；p 钳下界
  1e−12 → φ ∈ [0,12]；
- 样本 < 2 恒 0（无统计诚实）；读数：sampleCount / meanIntervalMillis；
- 契约：windowSize ≥ 2、minStdDevMillis > 0 fail-fast。

## User Stories

1. 作为健康作者，φ>1 预警、φ>4 判定失联——分档动作而非一刀切。
2. 作为 SRE，φ 曲线渐升可读——抖动（φ 尖峰回落）与真死（φ 持续
   攀升）形态可辨。
3. 作为调参者，meanIntervalMillis 读数即学习到的心跳节奏。

## Implementation Decisions

- 时间全由调用方传入（确定性可回放，无时钟注入依赖）；
- erf 用有理近似（检测器精度足用，不引依赖）。

## Testing Decisions

- 规律心跳（1000ms×10）：刚过均值 φ<2、5 均值未到 φ>4；φ 随沉默
  单调增；远超期钳上界 12；样本不足恒 0（含从未心跳）；std 下界使
  均值处 φ≈−log10(0.5)；窗滑动适应新节奏（1000ms→100ms 重学）；
  畸形三型 fail-fast。

## Out of Scope

- 不接具体健康端点/告警（分档阈值接线归后续轮）；
- 不做多模态分布建模（正态单峰假设的诚实边界）。

## Further Notes

- 与三探针分层（liveness/readiness/startup）正交：探针答「此刻通不
  通」，φ 答「连续多可疑」。
