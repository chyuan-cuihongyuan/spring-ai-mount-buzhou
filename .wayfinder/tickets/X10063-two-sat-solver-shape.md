---
id: X10063
title: X 会话 32 TwoSatSolver SCC 缩点 2-SAT 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

TwoSatSolver（core/concurrent，静态纯函数面）：solve(n,clauses)——蕴涵图 2n 顶点（±v 字面量）+KosarajuScc（X31 消费面）缩点+凝聚图 Kahn 拓扑定序赋值（x 真 iff 正字量分量后于负字量分量）；UNSAT 返回 Optional.empty；null 子句/越界字面量 fail-fast。
