---
id: U7237
title: U 会话 U19 Soundex 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

同音异形怎么归并？（spec 7018 / effort #7018 / U19）

## Resolution

**Soundex（core/metrics）**：发音部位映射+首字母保留+相邻同码折叠（元音断开/HW 不断开）+1+3 补零截断。
