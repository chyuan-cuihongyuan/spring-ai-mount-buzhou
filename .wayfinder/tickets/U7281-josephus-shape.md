---
id: U7281
title: U 会话 U41 JosephusPermutation 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

循环淘汰序怎么完整可审计？（spec 7040 / effort #7040 / U41）

## Resolution

**JosephusPermutation（core/metrics）**：完整出列序模拟保留；k=1 顺序退化；survivor 与末位一致；越域 fail-fast。
