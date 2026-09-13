# 812 — 观测管道内存限流器

> 来源：H 会话第 13 轮 = effort #812 / [T1125](../../.wayfinder/tickets/T1125-pipeline-memory-limiter.md) / [T1126](../../.wayfinder/tickets/T1126-pipeline-memory-limiter-verify.md) / impl 565。
> 借鉴：OpenTelemetry Collector memory_limiter。

## Problem

观测管道背压只看条数（queueCapacity）：一条超大 span 与一条小 event 同权——大 payload 批次照样能吃穿堆。内存维度的准入判定缺位。

## Solution

`PipelineMemoryLimiter`（observability）：

- **准入判定**：`tryAdmit(weight)` —— `inFlight + weight > max` 拒收（CAS 无锁循环；拒收不计入在途）；`release(weight)` 归账（防 0 下穿）。
- **口径**：admitted/refused/released/currentBytes/maxBytes；零/负权重恒过且不计数。
- **拒绝语义**：只发信号——丢弃/降级/背压决策归调用方（与「读数面不改行为」纪律一致：管道接入是显式装配步）。

## 兼容性

纯新增；AsyncObservabilityPipeline 零变更（接入为后续装配步——本轮交付判定脑与验证）。

## 诚实边界

权重估算归调用方；单上限无 soft/hard 两档（留位）；纯内存计数（无持久化语义需求）。
