---
id: T3188
title: 快速重传触发器的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3187]
created: 2026-09-17
---

## Question

FastRetransmitTrigger 合同（恰触/切换/清零/交替/畸形）怎么钉住？（spec 2043 / effort #2043 / R44）

## Resolution

**七用例一次全绿**（buzhou-resilience）：阈值 3 恰第 3 触发 /
a²b¹a³ 切换重计后触发（unique=3 dup=3 对账）/ 触发清零新一轮（阈值
2 两轮两触）/ 首信号计 1 不触 / 交替 a/b 十轮永不触发 / 阈值 5 恰
第 5 / 畸形四型（阈值 1、0、null、空白 id）fail-fast。
