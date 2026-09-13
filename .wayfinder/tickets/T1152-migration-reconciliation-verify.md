---
id: T1152
title: 会话迁移对账验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1151]
created: 2026-09-13
---

## Question

四维比对与重映射跳过语义如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 26 轮 = effort #825）：MigrationReconciliationTest 6 例——全对+轮次数 2/计数不符/轮次漂移/状态键缺失明细/重映射跳过/空导出。修复三连：缺 import×2、contains 链、断言子串。
