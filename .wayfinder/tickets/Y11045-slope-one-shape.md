---
id: Y11045
title: Y 会话 23 SlopeOne 协同过滤 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

SlopeOne（core/metrics，静态纯函数面）：predict(ratings,user,item)——线性偏离 dev_ji=mean(r_kj−r_ki) 共评者域+加权预测 (Σ(dev+r_uj)freq)/Σfreq；NaN 未评口径；无共评基座 fail-fast。
