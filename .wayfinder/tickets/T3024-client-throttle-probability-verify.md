---
id: T3024
title: 客户端自适应节流的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3023]
created: 2026-09-23
---

## Question)

拒发概率在健康/过载/极端/畸形下正确吗？（spec 1911 / effort #1911 / R112）

## Resolution`

**ClientThrottleProbabilityTest 4 用例全绿**（mvn -pl buzhou-core
test -Dtest=ClientThrottleProbabilityTest）：健康 100/90/K2 → 0；
过载 100/40/K2 → ≈0.198；极端 200/50/K2 → ≈0.498；掷骰边界两例；
畸形三型 fail-fast。
