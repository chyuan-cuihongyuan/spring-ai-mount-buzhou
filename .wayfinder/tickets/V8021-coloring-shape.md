---
id: V8021
title: V 会话 V11 GraphColoring 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-29
---

## Question

资源冲突的可行着色怎么有界构造？（spec 8010 / effort #8010 / V11）

## Resolution

**GraphColoring（core/concurrent）**：度降序（并列编号序
canonical）+邻居未占最小色贪心；colors/colorCount 双面；
Δ+1 上限承诺（非最优明示）；自环 fail-fast。
