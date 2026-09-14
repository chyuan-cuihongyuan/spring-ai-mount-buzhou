# 1095 — SkillAdmin×Search 可见性联动组合测试轮

> 来源：J 会话第 95 轮 = effort #1095（[T1645](../../.wayfinder/tickets/T1645-adminsearch-shape.md) / [T1646](../../.wayfinder/tickets/T1646-adminsearch-verify.md) / impl 847）。纯测试轮第十弹（R81/R82/R84/R86/R87/R88 先例）。

## Problem Statement

R85 SkillAdminApi（管理面）与 R57 SkillSearchTool（搜索面）联动——DB skill 发布/下架后的**管理→搜索计数联动**无验证：发布后搜索应命中、下架后应消失。

## 目标

新增 `SkillAdminSearchComboTest`（buzhou-skills）：create+publish 后搜索命中（hits 增）→ disable 后搜索消失（misses 增）——双读面计数联动一致 + 各自守恒。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 绑定策略可见性裁剪联动（另轴）。
