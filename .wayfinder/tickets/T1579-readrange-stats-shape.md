---
id: T1579
title: read_range 回读判定读面（ReadRangeStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1577
created: 2026-09-15
---

## Question

J 会话第 62 轮：spill 域回读工具的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：ReadRangeTool（模型分段回读溢出原文的通道）全路径零计数——回读频次与截断率不可见。回读量是 Spill 管线有效性的下游信号（溢出后模型是否真回来读原文、截断率多少）；与 store 层 ReadAuditTrail（回读审计日志——记录谁读了什么）不同轴：那是审计流水、这是调用结局分布，可共存。

形状裁决：`ReadRangeTool` 内静态 `AtomicLong` 六计数——calls（入口）/ reads（完整回读）/ truncatedReads（截断回读——单独分桶因「模型只拿到部分原文」是管线上限信号）/ parseRejects（坏 JSON）/ skillRejects（skill:// 资源 bytes-only 拒绝与未接线拒绝合桶）/ failures（catch 兜底）；嵌套 `record ReadRangeStats` + `stats()` + `resetForTest()`。守恒 `calls = reads + truncatedReads + parseRejects + skillRejects + failures`。静态面理由同族先例；call() 返回语义逐位不变。

Out of scope：按 path 分桶（敏感面红线）；回读字节量（R47 读侧字节口径已立，不重复）。
