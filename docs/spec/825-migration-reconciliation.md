# 825 — 会话迁移对账

> 来源：H 会话第 26 轮 = effort #825 / [T1151](../../.wayfinder/tickets/T1151-migration-reconciliation.md) / [T1152](../../.wayfinder/tickets/T1152-migration-reconciliation-verify.md) / impl 578。
> 借鉴：gh-ost 在线迁移对账（≈16K star）。

## Problem

SessionMigrator 迁移完只计一次数：「搬完了」不等于「搬对了」——消息丢尾/状态键缺失/轮次边界漂移静默发生。

## Solution

`MigrationReconciliation`（core.session，纯函数）：

- **四维**：消息计数 / 轮次范围 / 首尾消息 id（仅同 sessionId=keepIds 时比对——重映射跳过）/ 状态键数+缺失键明细。
- **报告**：Reconciliation(source/target 计数+countsMatch+mismatches)；明细封顶 8（超出「…（共 N 项）」汇总）。
- **入参**：迁移前的源导出 + 迁移后的目标导出（复用 exportSession——零新通路）。

## 兼容性

纯新增静态工具；SessionMigrator 零变更。

## 诚实边界

只读不修复；摘要/spill 域不比对（诚实范围声明）。
