# 836 — 半开探测成功率读数

> 来源：H 会话第 37 轮 = effort #836 / [T1173](../../.wayfinder/tickets/T1173-halfopen-probe-stats.md) / [T1174](../../.wayfinder/tickets/T1174-halfopen-probe-stats-verify.md) / impl 589。
> 借鉴：Resilience4j permitted probe 语义（扩散轮）。

## Problem

半开探测反复失败被打回 OPEN（811 已量化跳闸频次）：探测本身的成败分布与连续失败深度无读数——「半开是在恢复还是反复被打回」不可辨。

## Solution

`HalfOpenProbeStats`（resilience.ratelimit，纯读数）：

- **成败累计**：successes/failures + consecutiveFailures（成功清零）。
- **近窗成功率**：最近 20 次探测 boolean 环均值（近期行为灵敏）。
- **有界**：模型封顶 32+truncated；null 忽略。

## 兼容性

纯新增；断路器零变更。

## 诚实边界

不控制探测许可（归断路器）；streak 无自动惩罚；近窗率非加权。
