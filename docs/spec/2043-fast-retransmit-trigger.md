# Spec 2043 — 快速重传触发器（effort #2043，R44）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3187–T3188，impl 1594）。
> 借鉴：TCP fast retransmit（3 dup-ACK）——连续重复信号提前触发不等超时。

## Problem Statement

故障响应依赖超时（熔断慢窗 / 重试计时）：同一问题反复出现时，超时
阈值前的每个信号都在白等——「连续 N 个重复信号」本身已是足够强的
证据，应提前触发（TCP 不等 RTO，3 个重复 ACK 即重传）。

## Solution

`FastRetransmitTrigger`（buzhou-resilience circuit，synchronized 小临
界区，纯信号驱动无时钟）：

- `signal(id)`：与上一信号相同 → 连续重复 +1；不同 → 切换重计
  （uniqueSignals +1）；**连续达阈值（默认 3，TCP 惯例）即触发**
  （返回 true，计数清零——新一轮从零）；
- 读数：triggerCount / stats()（触发数/重复信号/切换信号/当前连续）；
- 契约：threshold ≥ 2（1 则首信号即触发失去语义）、id 非空 fail-fast。

## User Stories

1. 作为韧性作者，同签名失败连现 3 次即提前换道——不等慢超时窗口。
2. 作为对账者，uniqueSignals 高 = 问题漂移（每次都新签名）；dup 高 =
  问题聚焦——两类腐化分显形。

## Testing Decisions

- 阈值 3 恰第 3 触发；切换重计（a²b¹a³ 触发 + 双切计数）；触发清零
  新一轮；首信号不触；交替信号永不触发；阈值 5 恰第 5；畸形四型
  fail-fast。

## Out of Scope

- 不做超时兜底合并（纯信号口径）；不接熔断/重试链（接线归后续轮）。

## Further Notes

- 与错误签名聚类（#44）互补：聚类定签名形态，本件定触发节奏。
