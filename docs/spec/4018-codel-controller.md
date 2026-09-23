# Spec 4018 — CoDel 受控延迟队列（effort #4018，R19）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6037–T6038，impl 2119）。
> 借鉴：Nichols-Jacobson 2012 / RFC 8290；Linux fq_codel。

## Problem Statement

「bufferbloat：队列满才丢、尾丢成批同步」——**时间维度**的 AQM
（丢不丢看延迟不看水位）件缺失。

## Solution

`CoDelController`（core/backpressure，纯裁决 + 队列门面）：

- 只盯驻留时间（sojourn）是否超目标（target 5ms 级）；
- 超目标持续一个 interval（100ms 级）→ 进入丢包态首丢；此后
  间隔按 interval/√n 递缩持续早丢；回到目标下立即复位；
- 时钟可注入（确定性回放）；Queue<T> 便捷门面（offer/poll +
  dropped/passed/depth 账面）。

## User Stories

1. 作为流控作者，排队病早暴露——早丢小量打散同步突发。
2. 作为低速率链路作者，丢否按时间判——变速流量不受水位口径误伤。

## Testing Decisions

- 目标下过 + 回标复位；持续超载首丢（观察窗语义）+ 间隔递缩
  第二丢；队列门面陈头丢新头过 + 账面；百条新鲜流零丢全过；
  畸形五型 fail-fast。

## Out of Scope

- 不做字节/包长记账（条目口径）；不做 ECN 标记（丢包口径）；
  不做 fq_codel 流哈希分桶（单流语义）。

## Further Notes

- 与 RandomEarlyDrop（RED 水位概率）成 AQM 双档。Wave 4
 （队列与流控族）开波。
- 里程碑：19/50。
