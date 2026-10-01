---
id: X10091
title: X 会话 46 AdamOptimizer 自适应矩估计 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

AdamOptimizer（core/metrics，静态纯函数面）：minimize(objective,initial,lr,β1,β2,ε,iters)——一阶/二阶矩 EMA+bias 修正 m̂/v̂+w−=lr·m̂/(√v̂+ε)；嵌套 Objective 接口（valueAt/gradientAt）；超参域 fail-fast。
