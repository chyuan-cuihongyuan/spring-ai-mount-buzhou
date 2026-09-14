# 1417 — 舱壁在飞峰值水位读面

> 来源：L 会话第 18 轮 = effort #1417（票 T2135 / T2136 / impl 1070）。**换题记录**：原题 R24 checkpoint 年龄勘察发现 CompactionCheckpoints 状态无时戳（改格式有兼容风险不硬来）；R31 批量嵌入无批量 API（前提不成立）——换入 R25 题的峰值水位轴。借鉴：HikariCP 池饱和度（idle/active 之外，「池最忙时用到多少」是扩容依据）。

## Problem Statement

`AgentBulkhead`（spec 117）有 per-agent 拒绝榜（topRejections）与瞬时 `inFlight(agent)`，但**历史峰值**无读面：配额调大/错峰治理的依据是「这个 agent 最忙时离上限多近」——QUOTA_EXCEEDED 只记录失败时刻，容量规划无水位可依。

## 目标

- `AgentBulkhead`（core/concurrent）增量：
  - per-agent 峰值水位表（256 封顶折 `__overflow__`——同拒绝表纪律）；`recordPeak` 于 acquire 成功路径采样（limitOf − availablePermits）；
  - `peakInFlight(agent)`：历史最大并发 Turn 数（无记录 = 0）；
  - `peakSaturation(agent)` = 峰值/上限 ∈ [0,1]（1.0 = 曾打满；未配置上限的 NOOP 舱无采样恒 0、饱和度 -1 哨兵）；
  - 拒绝路径不采样（峰值只反映真实占位，不因拒绝虚高）。
- internal 类手术式扩展：无新公共类型（快照面不变），既有语义（QUOTA_EXCEEDED/热调整/拒绝榜）逐位不变。

## 兼容性

纯增量读面：acquire/Lease/热调整/拒绝语义逐位不变；峰值表与拒绝表同款有界纪律。

## Out of Scope

- 集群聚合面（BulkheadClusterAggregation 已有 reporter 族）。
- 饱和度告警/自动扩容联动（BulkheadScalingAdvisor 另族）。
- 时间窗峰值（全生命周期水位口径显式）。
