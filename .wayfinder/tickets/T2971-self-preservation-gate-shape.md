---
id: T2971
title: 自保模式门的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

大面积失联时剔除的停逐判定怎么语义化？（spec 1885 / effort #1885 / R86）

## Resolution`

**Eureka self-preservation 持态门 `SelfPreservationGate`
（core/concurrent）**：onRenewal 续约计数 + renewalRatio 比率读数 +
shouldExpire（比率 < 阈值停逐 / ≥ 阈值正常逐——剔除前必查）+
selfPreserving 自保态读数。实例数≥1/阈值∈(0,1) fail-fast；边界
恰等按正常逐（恢复不含糊）。
