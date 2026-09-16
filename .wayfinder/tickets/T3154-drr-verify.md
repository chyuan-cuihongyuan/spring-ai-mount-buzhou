---
id: T3154
title: 加权公平调度的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3153]
created: 2026-09-17
---

## Question

WeightedFairScheduler 合同（长期公平/交替/突发/清账/畸形）怎么钉住？（spec 2026 / effort #2026 / R27）

## Resolution

**七用例全绿**（两轮修复：首版无粘性轮内消费致长期比 1:1（权重失效
——单项出口每轮只兑现 1 项教训）；粘性后突发窗口断言改 getOrDefault
口径）：3:1 权重 300 项长跑比 ∈[2.7,3.3] / 单流独占排干 / 等权严格
交替 / quantum10×w5=50 突发 heavy 领先（light 可零服务） / 空闲流清
账再入从零 / 空返 null / 畸形七型 fail-fast。
