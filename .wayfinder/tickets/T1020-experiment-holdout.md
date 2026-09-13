---
id: T1020
title: 全局 holdout 层的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

跨实验控制组语义缺失——「实验组合的净效应」无法回答。加 holdout 层吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 11 轮 = effort #710 / spec 710 / impl 610）：构造器再扩 holdoutPercent（默认 0 零变化）——assign() 到期判定后、落桶前判 `sha256("holdout|unitKey")`（哈希不含实验名——层语义全局一致排除）→ null+__holdout__ 独立桶+holdout 计数；读数 holdoutPercent()。静态配置（动态调比例污染对照——刻意排除）。
