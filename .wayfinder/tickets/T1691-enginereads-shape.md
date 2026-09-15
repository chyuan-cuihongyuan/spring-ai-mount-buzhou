---
id: T1691
title: RangeReadEngine 引擎读面（EngineReadStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1675
created: 2026-09-15
---

## Question

J 会话第 116 轮：切片引擎的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：RangeReadEngine.read（切片引擎静态原语，read_range 工具与 spill 回读共用）零计数——引擎级调用分布（bytes/window/json 三模式占比）无口径（R62 是工具层端到端，引擎层是多调用方共享原语——不同轴）。

形状裁决：`RangeReadEngine` 内静态 `AtomicLong` 四计数——engineCalls（read 入口）/ windowReads（window 模式）/ jsonReads（json 模式）/ byteReads（bytes/默认模式）；嵌套 `record EngineReadStats` + `stats()` + `resetForTest()`。守恒 engineCalls = 三模式桶之和。静态面理由同族先例；read 返回语义逐位不变。

Out of scope：切片长度分布（展示面）；jsonPath 求值失败细分（失败已透传文本）。
