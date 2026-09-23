---
id: T6018
title: R 会话 R9 增量基准帧的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6017]
created: 2026-09-23
---

## Question

R9 合同怎么逐一验绿？（spec 4008 / effort #4008 / R9）

## Resolution

**验证通过**：DeltaFrameOfReferenceTest 五测全绿——千单调时间戳
roundtrip + 字节面 < 原 varint 1/3；两帧跳变重置 roundtrip；
负值/混沌/极值全域 roundtrip + 区段读；frameSize=1 退化档 + 空编码；
畸形六型 fail-fast（0 帧/null/截断/残留）。
