---
id: T6039
title: R 会话 R20 反放大窗的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

未验证对端的反射放大面怎么关？（spec 4019 / effort #4019 / R20）

## Resolution

**QuicAmplificationWindow（core/backpressure）**：QUIC RFC 9000
§8.1——信用窗（收 ×3 / 发扣减 / 超发 fail-fast）+ 初始授信覆盖
握手 + validateAddress 解除窗。与 HierarchicalTokenBucket（稳态
限速）互补：临时放大闸。
