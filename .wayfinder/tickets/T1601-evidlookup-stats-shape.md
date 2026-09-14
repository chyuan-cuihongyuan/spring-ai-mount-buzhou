---
id: T1601
title: evidence_lookup 证据回查读面（EvidenceLookupStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1599
created: 2026-09-15
---

## Question

J 会话第 73 轮：memory/tool 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：EvidenceLookupTool（evidence_id 证据回查通道）全路径零计数——回查频次、未找到率、切片率不可见。回查率是证据链引用有效性的下游信号（Redis cache hit-rate 思想：miss 率高=引用与存储失配）。

形状裁决：`EvidenceLookupTool` 内静态 `AtomicLong` 五计数——calls（入口）/ misses（未找到）/ hits（命中=completeReads+slicedReads）/ completeReads（全文返回）/ slicedReads（带截断标记返回——切片率是「模型需要精读但窗口不够」信号）；嵌套 `record EvidenceLookupStats` + `stats()` + `resetForTest()`。双守恒：`calls = hits + misses`、`hits = completeReads + slicedReads`。静态面理由同族先例；call() 返回语义逐位不变。

Out of scope：按 evidenceId 分桶（敏感面红线）；字节数口径（R47 已立）。
