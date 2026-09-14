# 1427 — 保留清扫新鲜度追踪器

> 来源：L 会话第 28 轮 = effort #1427（票 T2155 / T2156 / impl 1080）。借鉴：Airflow scheduler heartbeat（调度器心跳消失即数据过期——调度类作业的新鲜度是数据承诺的前提）。

## Problem Statement

`RetentionSweeper` 周期清扫失败/停摆时没人发现：报告只推 listener 不落水位——「上一次清扫是多久以前、间隔最长拉到多大、失败了几次」无读面。清扫停摆 = 会话/观测数据承诺失效的静默漂移。

## 目标

- `RetentionSweepFreshness implements Consumer<RetentionSweepReport>`（core/retention，opt-in 实例面）：
  - 经既有 `addSweepListener` 注册即生效（零 sweeper 改动——listener seam 先例）；
  - 快照四面：`sweepCount` / `lastSweepAt`（epoch；从未清扫 = -1）/ `staleMillis`（now − 末次；调用方时钟注入）/ `maxGapMillis`（相邻清扫间隔水位——调度抖动/停摆显形）+ `failureCount`（未完全成功次数）；
  - `freshness(Instant now)` 只读快照 + `resetForTest()`。
- 失败判定复用 `fullySucceeded()`（failures 非空即未完全成功）。

## 兼容性

纯 opt-in 读面：不注册零开销；不触 sweeper 调度/清扫语义。

## Out of Scope

- 清扫停滞告警联动（读面不裁决）。
- 各删除步骤的分布（RetentionSweepReport 已有逐字段）。
- 集群多实例清扫去重（单进程口径）。
