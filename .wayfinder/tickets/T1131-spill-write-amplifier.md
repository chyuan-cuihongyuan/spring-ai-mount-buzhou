---
id: T1131
title: Spill 写放大读数的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

写放大记账挂 store 还是独立脑？近窗与累计双口径如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 16 轮 = effort #815 / spec 815 / impl 568）：`SpillWriteAmplifier` 独立记账脑（零 store 侵入）——total counters+近窗 64 样本（均值+P95 最近秩）；逻辑 ≤0 忽略防 ∞；喂点归装配侧（同 810 模式）。
