---
id: T1643
title: compact×evidence 交叉组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1645
created: 2026-09-15
---

## Question

J 会话第 96 轮：压缩与证据回查交叉的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R59 compact_now（压缩折入）与 R73 evidence_lookup（按 evidence-id 回查消息）交叉——压缩折入不影响 MessageStore（append-only），evidence 回查计数语义在压缩前后一致。纯测试轮第十一弹。

形状裁决：新增 `CompactEvidenceComboTest`（buzhou-memory）——压缩前后 evidence 回查各一次：双读面计数一致（compact 与 evidence 互不干扰）、各自守恒保持。零生产改动。
