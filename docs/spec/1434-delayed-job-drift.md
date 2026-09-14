# 1434 — 延迟作业调度漂移读数

> 来源：L 会话第 35 轮 = effort #1434（票 T2171 / T2172 / impl 1087）。借鉴：Sidekiq queue latency（队列延迟是调度健康第一指标——任务「计划时刻 vs 实际起跑」的漂移量化调度线程饥饿）。

## Problem Statement

`DelayedJobQueue`（spec 413 延迟作业：到点执行一次）的**调度漂移**（实际起跑 − 计划 fireAt）无读面：调度线程被长任务占住/系统负载抖动时作业迟到，承诺的「到点执行」失守无信号——补偿逻辑（按 fireAt 计算的截止期）会系统性错过。

## 目标

- `DelayedJobQueue`（core/concurrent）增量：
  - submit 双重载的 task 包装为漂移记录版（执行起点采样 `clock.instant() − fireAt`，负值钳 0——时钟注入一致性）；
  - `DriftStats(executed, lastDriftMillis, maxDriftMillis)` + `driftStats()` 快照 + `resetDriftForTest()`；
  - 已过期提交（fireAt 已过 = 立即补跑）漂移显形为正值——补偿逻辑的错过窗口量化。
- 既有 submit/cancel/替换（旧任务不双跑）/failureObserver 语义逐位不变。

## 兼容性

纯增量读面：task 包装一层调度漂移记账，执行/替换/取消语义不变；漂移采样用注入 clock（与队列一致性）。

## Out of Scope

- 分位数（仅 last/max 水位——作业量级低，全量分布意义有限）。
- 阈值告警联动（读面不裁决）。
- scheduler 线程池尺寸调优建议（BulkheadScalingAdvisor 族另域）。
