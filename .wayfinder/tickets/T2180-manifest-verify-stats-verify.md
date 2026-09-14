---
id: T2180
title: 校验漏斗与明细桶的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2185
created: 2026-09-14
---

## Question

如何证明受踪包装透传与漏斗守恒？

## Resolution

**用户常设授权 AFK（可推翻）**

`ExportManifestVerifyStatsTest` 三测全绿（`mvn -pl buzhou-core -am test`）：不匹配内容 → failed=1/mismatched=1/lastEntryKind=full；末次入口追踪（canonical→subset 切换）；reset 归零（verifies=0+conserved+null）。评审修正：子集校验内容须为 JSON 对象（canonicalJson 解析约束）——测试输入改 "{}"。
