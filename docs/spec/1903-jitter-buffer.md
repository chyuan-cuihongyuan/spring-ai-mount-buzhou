# Spec 1903 — 自适应抖动缓冲（effort #1903，R104）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3007–T3008，impl 1504）。借鉴：
> VoIP/WebRTC（万星级）adaptive jitter buffer——事件到达间隔抖动时
> 播放端延迟 D 毫秒再出，D 按近期到达延迟的覆盖分位自适应：D 太小
> 事件未到即播（空转），D 太大延迟白白增加。

## Problem Statement

事件流消费端的出队节奏两头难：立即出队遇乱序/迟到就空转重试，
固定大延迟又让所有事件陪绑——「延迟多少能覆盖 target 比例的到达」
缺独立计算面。

## Solution

`JitterBuffer`（core/backpressure，静态纯函数）：

- `requiredDelay(arrivalDelays, targetCoverage)`：按近期到达延迟
  样本取覆盖 target 的最小延迟（升序第 ⌈target×n⌉ 个）；
- `coverageRatio(arrivalDelays, delayMillis)`：样本 ≤ delay 的占比
  ——给定延迟的实际覆盖读数。

## User Stories

1. 作为事件消费者，样本 {10..50} target 0.9 → 延迟 50ms——90%
   到达都在窗内。
2. 作为延迟调参者，coverageRatio(30)=0.6——固定延迟 30ms 只覆盖
   六成，加不加延迟有据。
3. 作为极端兜底者，target=1.0 → 取最大样本——一个都不空转。

## Implementation Decisions

- 纯函数零状态（样本窗口归调用方）；target ∈ (0,1]、样本非空且
  ≥ 0 fail-fast；索引 = ⌈target×n⌉−1（覆盖保证下取達标样本）。

## Testing Decisions

- 覆盖分位两例（target 0.9→50ms/target 0.5→30ms）；覆盖率两例
  （30→0.6/50→1.0）；target=1.0 极值；畸形三型 fail-fast。

## Out of Scope

- 不做真实出队调度（归消费端）；不做网络 RTT 估计。

## Further Notes

- 与 blocking backpressure（满队阻塞）互补：那是队列满的背压，
  这是时间轴上的抖动吸收。
