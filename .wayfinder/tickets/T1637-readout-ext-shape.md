---
id: T1637
title: 冒烟清单扩展轮（R78/R79 读面纳入）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1629
created: 2026-09-15
---

## Question

J 会话第 91 轮：读面冒烟清单的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R83 建立的 ReadoutContractSmokeTest 清单（13 成员）漏纳 R78 ArchivePurgeJob 与 R79 SpillCipher 两读面（建清单时该两轮未完成或遗漏）——登记纪律要求清单随读面产出同步扩展。纯测试轮。

形状裁决：清单追加 `ArchivePurgeJob`（R78）与 `SpillCipher`（R79）两成员——17 成员全量冒烟。零生产改动。
