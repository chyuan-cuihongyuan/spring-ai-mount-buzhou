---
id: T2372
title: R11 离群驱逐接线的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2371
created: 2026-09-15
---

## Question

N 会话第 11 轮：如何验收？

## Resolution

ModelOutlierEjectionCategoryTest 四断言（默认集过滤+复池 / 自定义集+大小写 /
成功复位 / 装配转换）。resilience 全量 377 用例零回归（既有 ejection/panic 测试
在旧单参 recordError 委托 SERVER 下语义保持）。
