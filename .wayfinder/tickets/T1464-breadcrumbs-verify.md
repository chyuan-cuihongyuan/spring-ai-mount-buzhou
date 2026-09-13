---
id: T1464
title: 会话面包屑环形读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1463
created: 2026-09-14
---

## Question

J 会话第 7 轮：面包屑环形读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（BreadcrumbRingTest，core.internal.session 同包直构）：record 新→旧序；有界（灌 CAPACITY+8 留 32、队首最新）；snapshot 不可变；clear 清空；epochMillis 非负。接线由 DefaultAgentSession.deliverEvent 单语句承载（编译 + 既有会话回归兜底，直构会话无测试先例——诚实入档）。定向 `mvn -pl buzhou-core test -Dtest='BreadcrumbRingTest,EventDropBreakdownTest'` 绿（后者同包分发器回归）。
