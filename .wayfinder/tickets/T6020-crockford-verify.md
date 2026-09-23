---
id: T6020
title: R 会话 R10 Crockford Base32 的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6019]
created: 2026-09-23
---

## Question

R10 合同怎么逐一验绿？（spec 4009 / effort #4009 / R10）

## Resolution

**验证通过**：CrockfordBase32Test 四测全绿——锚点（0/"0"、31/"Z"、
32/"10"、"C1S"↔12345）+ Long.MAX_VALUE 13 位往返；形近归一六型
（o→0/i→1/l→1/小写/连字符/混写）；校验符往返（含极值）+ 尾符
篡改必捕；畸形七型 fail-fast。首版 digitOf 映射笔误（I/L 误并
J=18）已按字母表全表修正。
