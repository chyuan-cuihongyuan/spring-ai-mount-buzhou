---
id: T1137
title: 限流自适应收紧器的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

429 驱动的放行收紧放 backend 还是独立乘数器？恢复语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 19 轮 = effort #818 / spec 818 / impl 571）：`AdaptiveRateTightener` 独立乘数器——429 乘性收缩+下限、保持窗+乘性步进恢复纯时间推导（无线程确定性）；乘数接线归调用方；封顶 32；五参 fail-fast。
