# Spec 549 — guard 装配摘要读数（effort #549）

> wayfinder map：`.wayfinder/maps/effort-549.md`（T859-860）。E 会话第 49 轮。

## Problem Statement

guard 装配了哪些 hook 只能翻 builder 代码——「guard 到底挂了哪些钩子」
是支持/排障的第一问，无读数面。

## Solution

`GuardModule.assemblySummary()`：hook 名列表（装配序）；未知 yml 键
宽容忽略（错键治理归 config doctor 面）。

## User Stories

1. 作为支持工程师，我想一屏看到 guard 挂了哪些钩子， so 排障第一问
   不用翻装配代码。

## Implementation Decisions

- 未知 yml 键宽容忽略（guard 错键治理归 doctor 面——本面只管读数）。

## Testing Decisions

- 装配多 hook → 摘要含全部名；disabled 模块摘要空；未知键宽容。

## Out of Scope

- doctor 化未知键；per-hook 配置详情。

## Further Notes

- 无新顶层公共类型（加法方法）——快照零 diff 预期。
