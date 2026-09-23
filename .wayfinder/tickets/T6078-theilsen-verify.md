---
id: T6078
title: R 会话 R39 Theil-Sen 稳健斜率的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6077]
created: 2026-09-24
---

## Question

R39 合同怎么逐一验绿？（spec 4038 / effort #4038 / R39）

## Resolution

**验证通过**：TheilSenSlopeTest 五测全绿——精确线复原
（3/2）；单点 +1000 污染 TS 稳 OLS 偏移对照；手算四点例
（下中位 10/截距 0）；竖直退化/不齐/n<2/非有限 fail-fast。
