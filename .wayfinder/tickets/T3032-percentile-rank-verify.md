---
id: T3032
title: 百分位排位的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3031]
created: 2026-09-23
---

## Question)

排位在中位/极值/畸形下正确吗？（spec 1915 / effort #1915 / R116）

## Resolution`

**PercentileRankTest 3 用例全绿**（mvn -pl buzhou-core test
-Dtest=PercentileRankTest）：样本 {1..5} 值 4 排位 0.8；最大 1.0
最小 0.2；百分位直读 80；畸形两型 fail-fast。
