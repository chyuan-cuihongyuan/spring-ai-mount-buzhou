---
id: T5014
title: Q 会话 R7 P² 流式分位数的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5013]
created: 2026-09-18
---

## Question

R7 合同怎么逐一验绿？（spec 3006 / effort #3006 / R7）

## Resolution

**验证通过**：PSquareQuantileTest 八测全绿——洗牌千整数 p50
499.5±5 / p90 899.1±10（种子确定性）、常量流精确、单调升千数
500±10（端标记+逐步逼近 torture）、前 5 样本 {30,10,20}→20
诚实口径、空态 NaN、p 开区间四路 fail-fast、重复值密集流标记
有序不越界。
