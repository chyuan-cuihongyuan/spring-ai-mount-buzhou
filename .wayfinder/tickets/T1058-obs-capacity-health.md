---
id: T1058
title: 内存观测库容量健康面的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

观测逐出静默发生无读数——加容量健康面吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 30 轮 = effort #729 / spec 729 / impl 629）：store 加 evictedSessions 计数+三读数（sessionCount 提升 public）；`ObservabilityCapacityHealth` mechanism=memory-observability 恒 UP——details used/max/utilization/evicted。告警归 312 自裁（观测可再生数据 DOWN 不合适）。
