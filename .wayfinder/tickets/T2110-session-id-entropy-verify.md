---
id: T2110
title: SessionIdEntropyAudit 分档与下界口径的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2109
created: 2026-09-14
---

## Question

如何证明 UUID/时间戳/可猜串的分档与字母表下界单调性？

## Resolution

**用户常设授权 AFK（可推翻）**

`SessionIdEntropyAuditTest` 六测全绿（`mvn -pl buzhou-core -am test`）：UUIDv4 → STRONG（≥112 bits，字母表下界 37=26+10+1）；时间戳纯数字 → WEAK（alphabet=10）；user123 → WEAK（36）；null/空/空白 → INVALID；同长度类越全熵越高（单调链）；批量四桶计数（含 moderate/strong 合并断言）。
