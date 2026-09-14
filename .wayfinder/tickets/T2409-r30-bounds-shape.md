---
id: T2409
title: R30 输入边界四护栏的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2408
created: 2026-09-15
---

## Question

N 会话第 30 轮：护栏静态常量还是 yml 可配？

## Resolution

选 **静态常量**（settings 先例——护栏的合理性不随部署变化：64K body/8K URL
是 HTTP 生态通用界）。超长 body 的正道是 bodyPath Onload 通道（框架加载），
拒绝文案带修法指引。单头超限独立异常分支——输入护栏与执行失败分桶（守恒式
语义准确）。
