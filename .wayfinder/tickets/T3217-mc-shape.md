---
id: T3217
title: 多重比较校正的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

多指标检验的族错误率怎么控制？（spec 2058 / effort #2058 / R59）

## Resolution

**Bonferroni/Holm 纯函数 `MultipleComparisonCorrection`（core/eval）**：
bonferroni（p×m≤α 最保守单步）+holm（p 升序逐步比 α/(m−j+1) 首次
不显著即止——FWER 同控功效恒不弱于）+Verdict 索引序显著+m=1 退化
原始口径——多指标 A/B 的「显灵」指标控制件。
