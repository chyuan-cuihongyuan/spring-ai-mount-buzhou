---
id: Y11021
title: Y 会话 11 DurbinLevinson AR 递推 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

DurbinLevinson（core/metrics，静态纯函数面）：solve(r)——自相关序列 [r1..rp] 递推 AR(p) 系数 a[1..p]+预测误差方差 E（嵌套 ArModel record）；|r1|≥1/越序 fail-fast。
