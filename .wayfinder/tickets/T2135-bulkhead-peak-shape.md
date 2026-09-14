---
id: T2135
title: 舱壁在飞峰值水位（AgentBulkhead 增量）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 18 轮（换题轮）：舱壁容量规划水位面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察换题：R24 checkpoint 无时戳（不硬改格式）；R31 无批量 API（前提不成立）——换入 R25 峰值轴（AgentBulkhead 有拒绝榜/瞬时 inFlight，无历史峰值）。

形状裁决：AgentBulkhead 手术式增量——per-agent 峰值表（256 折叠纪律同拒绝表）+acquire 成功路径采样（拒绝不采样不虚高）+peakInFlight/peakSaturation（-1 哨兵 unlimited）+internal 扩展无新公共类型；既有语义逐位不变。

Out of scope：集群聚合；告警联动；时间窗峰值。
