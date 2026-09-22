---
id: T3020
title: 闲谈收敛估算的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3019]
created: 2026-09-23
---

## Question)

收敛估算在经典/反解/极值/畸形下正确吗？（spec 1909 / effort #1909 / R110）

## Resolution`

**GossipConvergenceTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=GossipConvergenceTest）：1024/3→5 轮；知情数封顶 N；反解
1000/10→2；N=1 零轮；畸形三型 fail-fast。
