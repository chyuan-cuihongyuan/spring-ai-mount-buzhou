---
id: T1581
title: 双时序事实台账操作读面（FactLedgerStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1577
created: 2026-09-15
---

## Question

J 会话第 63 轮：memory/summary 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：BiTemporalFactLedger（双时序事实台账）全路径零计数——废止记录写入量、历史/时点查询频次不可见；尤其 load 损坏 JSON 静默吞（catch 返回空表）= 台账记录静默蒸发。bitemporal 表 query/mutation 对账思想（Kleppmann《DDIA》双时序维护语义）。

形状裁决：`BiTemporalFactLedger` 内静态 `AtomicLong` 四计数——supersededWrites（recordSuperseded 写入）/ historyLookups（historyOf）/ validAtLookups（validAt）/ corruptRecordLoads（load 解析失败静默丢弃——蒸发显形核心桶）。四计数为三类操作独立量（操作类型不同不设统一守恒，每类口径注释说明）；嵌套 `record FactLedgerStats` + `stats()` + `resetForTest()`。静态面理由同族先例；三方法返回语义逐位不变。

Out of scope：按 section 分桶（节名是配置面，分布留后续轮）；validAt 命中/未命中细分（时点谓词结果面）。
