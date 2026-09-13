# 808 — 导出内容去重统计

> 来源：H 会话第 9 轮 = effort #808 / [T1117](../../.wayfinder/tickets/T1117-export-dedupe-stats.md) / [T1118](../../.wayfinder/tickets/T1118-export-dedupe-stats-verify.md) / impl 561。
> 借鉴：restic dedupe stats（≈31K star）。

## Problem

导出体积审计（738）回答「大在哪」，不回答「重复浪费了多少」：模板化回复/重复系统提示/相同错误文本在导出中大量冗余——去重潜力不可见。

## Solution

`ExportDedupeStats`（core.export，纯函数）：

- **精确重复口径**：去重键=块精确串值（MD5/相似度归其他族——口径显式）。
- **节省读数**：duplicateChars = Σ(count−1)×len；savingsRatio = duplicate/total。
- **Top 16**：按浪费字符降序，preview 截 32 字符（不暴露全文——隐私口径同导出脱敏族）。
- **空块口径**：null/空白计 totalItems 不计重复（输入即事实）。

## 兼容性

纯新增静态工具（喂消息正文/摘要段/状态值列表皆可）；无配置键。

## 诚实边界

字符长度口径（与 738 一致、非 UTF-8 字节精确）；只统计不打包（实际去重存储是存储层域）；精确匹配会低估（相似模板不算重复——诚实下界）。
