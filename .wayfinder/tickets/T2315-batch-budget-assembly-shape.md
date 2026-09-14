---
id: T2315
title: 批预算装配链测试（R29 补账）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2303
created: 2026-09-15
---

## Question

M 会话第 36 轮：spec 1526 的装配链（buzhouBatchResponseBudgetAdapter）测试如何补？

## Resolution

**用户常设授权 AFK（可推翻）**

IdempotentRetryAssemblyTest 同批三用例：值声明即 Holder 生效（5000 透传）；显式 0 仍装配 bean 但语义为关（enable 内 clamp——条件命中与语义开关解耦断言）；缺省无 bean 零装配。M 系产出的三个 Holder 装配（危险桥 R9/瞬断重试 R35/批预算本轮）测试全覆盖。
