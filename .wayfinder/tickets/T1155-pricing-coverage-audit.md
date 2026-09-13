---
id: T1155
title: 定价表覆盖审计的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

覆盖对账做进 PricingTable 还是独立纯函数？匹配语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 28 轮 = effort #827 / spec 827 / impl 580）：`PricingCoverageAudit` 独立纯函数（PricingTable 零变更）——三层匹配（精确/大小写/provider 前缀剥离）+覆盖率+unknown 典序封顶 32+空真语义（空调用 1.0、空表 0）。
