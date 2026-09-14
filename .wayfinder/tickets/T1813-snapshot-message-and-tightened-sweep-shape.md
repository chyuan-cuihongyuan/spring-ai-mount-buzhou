---
id: T1813
title: SnapshotMessage 补测与收紧判据跨模块复核（R5 形态）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 5 轮：R4 复核在 miss≥1 口径下再浮出 `SnapshotMessage`（mis=2，compact 构造 null 防御分支）——补测面是什么？收紧判据（miss≥1）是否需要跨模块复核？残留归口规则如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 5 轮 = effort #1204 / spec 1204 / impl 907）：

1. **SnapshotMessage 补测面**：compact 构造 null 防御合同——null metadata → 空 Map（读面「模型当时看到什么」的还原格式不可 null）；传入 metadata 防御拷贝（构造后外部 mutation 不透传）+ spillUri/evidenceId 字段透传。
2. **残留归口规则**：miss≥1 收紧判据下浮出的小类逐个「补测或显式豁免」，不许静默；纯 record 防御分支（本类 2 行）补测成本 < 豁免说明成本，补测。连续两轮（R4/R5）在该口径下 core 浮出量 = 2 + 1，呈收敛态——R6 起该口径的增量复核并入周期性对账轮，不再单开专轮。
3. **跨模块复核**：收紧判据对全部小模块（tools / observability / observe-otel / observe-dashboard / spill / resilience）在隔离 worktree 重扫一轮（现有报告 9/13–9/14 生成于旧判据期）——浮出项归 R6+；store-jdbc/store-redis（H2/fake 本地实测 96–99% 健康）与 memory/guard/skills（9/15 新鲜）不在复核列。
4. **边界**：不改主代码；匿名片段豁免口径不变。
