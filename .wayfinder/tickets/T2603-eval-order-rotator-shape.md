---
id: T2603
title: 评测项轮换消序的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

评测项换序的形状怎么裁决？（spec 1701 / effort #1701 / R2）

## Resolution

**静态纯函数 `EvalOrderRotator`（core/eval）**：`permutation(n, runIndex)` =
runIndex 派生种子的 Fisher–Yates 置换——种子用 JVM 规范固定算法的
`java.util.Random`（跨 JVM 重现，区别于 ThreadLocalRandom）；`shuffled`
重排新列表多重集守恒；`OrderPlan(runIndex, permutation)` 供审计复现。
借鉴 OpenAI Evals / HELM 种子化评测顺序。纯读面，与 EvalRunner 解耦。
