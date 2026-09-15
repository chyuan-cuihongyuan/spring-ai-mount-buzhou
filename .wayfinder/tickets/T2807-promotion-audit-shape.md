---
id: T2807
title: 记忆层代晋升审计的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question

记忆分层（微压缩→摘要→归档）配置对不对怎么量化？（spec 1803 / effort #1803 / R4）

## Resolution

**JVM 分代晋升诊断纯读面 `MemoryPromotionAudit`（buzhou-memory）**：
`CycleFacts(micro, promoted, archived, retained)` 单周期事实（构造器核非负+
三去向之和=产出契约）；`analyze` → PromotionReport（四总计 + promotionRate/
directArchiveRate 无产出 -1 哨兵 + prematurePromotionCycles 过早晋升计数——
有产出且零原地保留的轮）。晋升率常高=微压缩没拦住短命内容；过早晋升堆积
=年轻代缓冲失效。

