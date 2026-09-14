---
id: T2410
title: R30 输入边界四护栏的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2409
created: 2026-09-15
---

## Question

N 会话第 30 轮：如何验收？

## Resolution

HttpRequestInputBoundsTest 五断言：body 超限拒（含 bodyPath 指引）+ oversize
桶；URL 超限拒 + urlRejects 桶；头数量超限拒；单头值超限拒且 failures=0；
合规输入（正常 header）零影响。tools 118 用例零回归。
