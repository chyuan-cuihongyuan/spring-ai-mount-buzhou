---
id: U7233
title: U 会话 U17 Manacher 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

最长回文怎么线性？（spec 7016 / effort #7016 / U17）

## Resolution

**Manacher（core/metrics）**：分隔符统一奇偶+镜像右界
复用 O(n)；并列取起点最小 canonical；含 # fail-fast。
