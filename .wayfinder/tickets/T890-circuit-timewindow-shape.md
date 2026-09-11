---
id: T890
title: 熔断时间窗衰减的形态裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

resilience4j 的滑动窗有 COUNT/TIME 两型；本仓熔断是 count 环形窗——低频调用下陈年失败永久占窗（半小时前的 5 次失败仍能让下一失败跳闸）。时间窗怎么加？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 21 轮 = effort #600 / spec 620 / impl 473）：

1. `circuit.time-window`（默认 0 = count 窗既有语义零变化；正时长 = 老样本（now−at ≥ window）出率计算**与 min-calls 门**）。
2. 实现取「时间过滤」而非「时间桶」：ring 窗保留不变（容量语义不动），rate 计算时按时间戳过滤新鲜样本——实现小、语义直观、与 count 窗共存。
3. 样本时间戳与 window 平行 ring 记录（注入 Clock 驱动——测试确定性同 spec 41）。
4. 跳闸/复位/半开/共享闸语义全部不动（只改 CLOSED 态的率计算输入）。
