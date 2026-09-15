---
id: T2889
title: 重连退避阶梯的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

断线重连的「退避/封顶/放弃」三段式怎么统一？（spec 1844 / effort #1844 / R45）

## Resolution`

**Resilience4j retry/libpq 重连惯例思想纯策略 `ReconnectBackoffLadder`
（buzhou-mcp）**：delayMillis = min(base×multiplier^(attempt−1), cap)——
溢出安全（触顶先判后乘不做天文乘法）；verdict(attempt, maxAttempts) →
RETRY/GIVE_UP（边界含：第 maxAttempts 次仍 RETRY）；畸形四型 fail-fast。
纯算延迟零执行，与 McpServerBreaker（熔断状态机）正交。

