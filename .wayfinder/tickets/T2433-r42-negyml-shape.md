---
id: T2433
title: R42 负缓存 yml 装配的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2432
created: 2026-09-15
---

## Question

N 会话第 42 轮：装配用什么生命周期形态？

## Resolution

选 **DisposableBean 关闭钩子**（RetryBudgetAdapter 同款先例）。上下文关闭
停用 Holder——测试隔离（ApplicationContextRunner 多上下文）无静态残留；
已包装会话的缓存随 TTL 自然过期（无中断语义需求）。
