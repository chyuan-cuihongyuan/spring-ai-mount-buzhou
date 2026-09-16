---
id: T3168
title: 维护触发器的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3167]
created: 2026-09-17
---

## Question

MaintenanceTrigger 合同（阈值界/全死/兜底/记账/畸形）怎么钉住？（spec 2033 / effort #2033 / R34）

## Resolution

**七用例一次全绿**（buzhou-core）：10%/19% 不触 / 恰 20% 触（>=）与
50% 触 / live=0 dead>0 必触与双零不触 / 间隔 500 不触 1000 触 / 从未
触发 lastTriggeredAt=-1 自 0 起算 / 触发后间隔重算+计数 / 畸形六型
（阈值 0、1.1、间隔 0、负计数 ×2、负时刻）fail-fast。
