---
id: T1645
title: SkillAdmin×Search 可见性联动组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1643
created: 2026-09-15
---

## Question

J 会话第 95 轮：skills 管理与搜索联动的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R85 SkillAdminApi（管理面）与 R57 SkillSearchTool（搜索面）联动——DB skill 发布后搜索可见、下架后消失的**管理→搜索计数联动**无验证。纯测试轮第十弹。

形状裁决：新增 `SkillAdminSearchComboTest`（buzhou-skills）——create+publish 后搜索命中（hits）→ disable 后搜索消失（misses）——双读面计数联动一致 + 各自守恒。零生产改动。
