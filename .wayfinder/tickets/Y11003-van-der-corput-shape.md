---
id: Y11003
title: Y 会话 2 VanDerCorput 逆根序列 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

VanDerCorput（core/metrics，静态纯函数面）：sequence(base,count)——index 数位逆序小数化 ψ_b(i)∈[0,1)；基 2 分层均匀圣像；base≥2/count≥0 fail-fast。
