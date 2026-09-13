---
id: T1151
title: 会话迁移对账的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

迁移对账比对哪些维、重映射语义下 id 边界怎么处理？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 26 轮 = effort #825 / spec 825 / impl 578）：`MigrationReconciliation` 纯函数——计数/轮次范围/首尾 id（仅 keepIds）/状态键四维；明细封顶 8+汇总；只读不修复；SessionMigrator 零变更。
