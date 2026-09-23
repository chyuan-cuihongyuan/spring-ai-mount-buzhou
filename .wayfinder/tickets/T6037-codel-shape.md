---
id: T6037
title: R 会话 R19 CoDel 的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

队列丢不丢怎么按延迟而非水位判？（spec 4018 / effort #4018 / R19）

## Resolution

**CoDelController（core/backpressure）**：RFC 8290——sojourn 超目标
持续一个 interval 进丢包态首丢、间隔 interval/√n 递缩持续早丢、
回标立即复位；时钟可注入 + Queue 门面（dropped/passed/depth）。
与 RED（水位口径）成 AQM 双档。
