# Spec 1730 — 文件写型分类读面（effort #1730，R31）（effort #1730，R31）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2661–T2662，impl 1330，impl restic 备份变更分类（新增/未变/已变））。借鉴：WriteFileTool 的写入画像不可见：新建还是覆盖、覆盖有没有实际变化——「全是不变覆盖」= 模型在空转写，浪费 token 与 IO。

## Problem Statement

`FileWriteKindStats`（tools/file，实例面线程安全）：WriteKind 三闭集（CREATE/OVERWRITE_UNCHANGED/OVERWRITE_CHANGED）+record+census+changedShare=(creates+changed)/total（无样本 −1）+resetForTest。分类由写入路径判定喂入——纯读面。

## Solution

作为治理者，unchanged 覆盖占比高 → 模型在空转写，提示词纠偏。

## User Stories

1. 17300
2. 17301
3. 17302

## Implementation Decisions

- 17303

## Testing Decisions

- 17304

## Out of Scope

- 17305

## Further Notes

- 17306
