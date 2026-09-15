---
id: T1681
title: evidence×episodic 独立性组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1679
created: 2026-09-15
---

## Question

J 会话第 111 轮：evidence 与情景记忆独立性的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R73 EvidenceLookupTool（回查）与 R55 EpisodeLedger（情景记忆）双读面同会话独立——回查不写情景、情景 recall 不动消息存储。纯测试轮第十六弹。

形状裁决：新增 `EvidenceEpisodicComboTest`（buzhou-memory）——交叉调用后双读面各自计数独立（互不串账）+ reset 独立。零生产改动。
