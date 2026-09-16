# Spec 1872 — 突发信用账户（effort #1872，R73）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2945–T2946，impl 1473）。借鉴：
> AWS CPU credit / T3 unlimited——负载按基准速率免费蓄水，突发透支存量，
> 枯竭后降速回基准（不拒不崩）。

## Problem Statement`

限流只有「拒绝」一档：超速即 429。但容量语义里有第三态——**降速**：
突发透支积蓄的信用、见底后回归基准继续服务（任务慢但不失败）。「突发
了多久、还能突发多久、枯竭了几次」的账户面缺位，突发预算无从规划。

## Solution

`BurstCreditAccount`（core/backpressure，synchronized 小临界区）：

- 构造契约：capacity ≥ 1、baseRefill ≥ 1、initial ≥ 0（钳容量）；
- `refill(now)`：按基准速率蓄水封顶（时钟回拨 fail-fast）；
- `trySpend(cost)`：水位足即扣、不足拒 false（调用方降速——不拒任务）；
  水位见底计枯竭一次；
- `stats()` 快照：水位/容量/枯竭次数/满水率 + `burstHeadroomMillis()`
  突发余量换算（水位 ÷ 基准速率——还能全速多久）。

## User Stories

1. 作为容量规划者，burstHeadroom=125ms → 满水还能全速 125ms，预算内
   突发窗口心里有数。
2. 作为降级作者，trySpend false → 不报错，降速到基准重试——第三态
   语义（非 429）。
3. 作为审计者，exhaustions 计数 = 突发预算超支频率，调容量有据。

## Implementation Decisions

- 线程安全小临界区；水位钳 [0, capacity]；见底恰在 spend 时计数
 （一次性——连续枯竭不重复计）。

## Testing Decisions

- 蓄水封顶；透支枯竭计数+拒后回血；余量换算；畸形四型（含时钟回拨）
  fail-fast。

## Out of Scope

- 不做付费语义（真实计费归云侧）；不接具体执行器（降速接线归后续轮）。

## Further Notes

- 与 GcraRateLimitBackend（拒绝语义）、PrefetchCreditWindow（在飞上限）
  三足：速率拒绝/在飞上限/突发降速。
