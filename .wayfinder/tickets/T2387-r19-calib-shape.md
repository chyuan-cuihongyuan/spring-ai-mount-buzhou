---
id: T2387
title: R19 校准审计接线的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2386
created: 2026-09-15
---

## Question

N 会话第 19 轮：对账挂点选哪——注入视图构建处（memory）还是 afterModel（core）？

## Resolution

选 **afterModel 同点**。注入视图构建时的估算值与模型实际收到的 prompt 之间还有
工具结果注入等变化（跨时刻对账引入第二个偏差源）；afterModel 同点对当前 prompt
重估（估算器纯函数）与真实 usage 成对——校准的就是估算器本身。纯记账有界零
预算影响，恒挂。
