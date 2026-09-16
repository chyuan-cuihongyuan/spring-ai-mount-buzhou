---
id: T3125
title: 抢占重算账本的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

抢占决策的代价/收益怎么对账？（spec 2012 / effort #2012 / R13）

## Resolution

**vLLM preemption 线程安全双面账 `PreemptionLedger`（core/exec）**：
recordPreemption（victim 浪费面 + 抢占方收益面同笔入账）+
recordRecomputation 幂等 + netBenefitTicks（负=降阈值信号）+
wasteRatio 浪费率 + recomputeRate 重算率 + stats 快照。抢占阈值由拍
脑袋变读账定夺。
