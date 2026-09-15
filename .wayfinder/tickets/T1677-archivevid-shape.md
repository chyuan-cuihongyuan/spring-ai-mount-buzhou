---
id: T1677
title: 归档×evidence 回查联动组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1675
created: 2026-09-15
---

## Question

J 会话第 109 轮：归档与回查联动的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R75 SessionArchiver（归档级联删除）与 R73 EvidenceLookupTool（回查）联动——归档后回查行为的组合验证。纯测试轮第十六弹。

形状裁决：新增 `ArchiveEvidenceComboTest`（buzhou-core 或 memory）——归档成功后回查归档会话的 evidence（miss 或命中按 store 语义）+ 双读面守恒。零生产改动。
