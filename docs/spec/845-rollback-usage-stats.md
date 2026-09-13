# 845 — Prompt 回滚使用读数

> 来源：H 会话第 46 轮 = effort #845 / [T1191](../../.wayfinder/tickets/T1191-rollback-usage-stats.md) / [T1192](../../.wayfinder/tickets/T1192-rollback-usage-stats-verify.md) / impl 598。
> 借鉴：S6 备选池（Langfuse rollback 观测面）。

## Problem

prompt 回滚有版本机制（PromptVersion）但无使用统计：哪些 prompt 在反复回滚（不稳定提示温度计）不可见。

## Solution

`RollbackUsageStats`（core.prompt，纯记账）：record(name, from, to, atMs)——次数/最近版本对/lastSeen 聚合；名封顶 64+溢出桶；次数降序快照；喂点=回滚执行处装配侧。

## 兼容性

纯新增；PromptRegistry 零变更。

## 诚实边界

只统计不执行回滚；溢出桶不可回溯；内存有界。
