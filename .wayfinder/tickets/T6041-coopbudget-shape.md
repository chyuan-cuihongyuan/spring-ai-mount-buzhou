---
id: T6041
title: R 会话 R21 协作预算的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

单任务长循环霸占 worker 怎么从运行时根除？（spec 4020 / effort #4020 / R21）

## Resolution

**CoopBudget（core/concurrent）**：Tokio coop——调度获 N 点预算、
关键操作 charge 扣 1、尽即让出（yield 重置满额）、尽后再扣
fail-fast。纯预算账（真调度归运行时）。与 SpawnGate（准入）互补：
进了之后霸不霸。
