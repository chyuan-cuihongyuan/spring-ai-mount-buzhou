# Spec 545 — 提示词注册表快照导出/导入（effort #545）

> wayfinder map：`.wayfinder/maps/effort-545.md`（T847-848）。E 会话第 45 轮。

## Problem Statement

注册表版本/标签只在内存/DB——跨环境搬运（备份/还原）无序列化面。

## Solution

`prompt.PromptRegistrySnapshot`（401 扩散；Langfuse export/import 思想）：

- export(registry) → 可移植 JSON（format 标记 buzhou.prompt-registry-
  snapshot + names 字典序 + 版本升序 + labels 全量指针）。
- importInto(registry, json) → 导入到**空注册表**按旧版本序重放 publish
  （新版本号与旧一致——复现锚）+ 标签重指（latest 自动跳过）；非空
  目标/格式不符 fail-fast。
- publishedAt 不保真（重放时刻为准——版本号才是复现锚）。

## User Stories

1. 作为宿主，我想备份注册表快照并在新环境还原， so 提示词资产可迁移
   可灾备。

## Implementation Decisions

- 仅空注册表导入（合并语义复杂度不抵收益——备份/还原足用）。
- names 字典序重放（确定性——新版本号与旧一致）。

## Testing Decisions

- 导出还原往返（版本号/正文/标签指针保持）；非空目标 fail-fast；
  坏 JSON/格式不符 fail-fast；空注册表导出合法。

## Out of Scope

- 非空合并；publishedAt 保真。

## Further Notes

- 新公共类型 `PromptRegistrySnapshot` 随轮 regenerate 快照 +
  api-surface.md 加行。
