---
id: Y11005
title: Y 会话 3 WalshHadamard 变换 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

WalshHadamard（core/metrics，静态纯函数面）：transform(x) 原位蝶形（±1 蝶宽 1 起逐层倍增）非归一 Walsh–Hadamard 变换；2 幂长约束+对合面（H(H(x))=n·x）+null/非 2 幂/非有限 fail-fast。
