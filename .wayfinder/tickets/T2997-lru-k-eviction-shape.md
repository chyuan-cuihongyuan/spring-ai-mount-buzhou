---
id: T2997
title: LRU-K 驱逐的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

倒数第 K 次访问的驱逐准则怎么语义化？（spec 1898 / effort #1898 / R99）

## Resolution`

**Postgres LRU-K 持态 keeper `LruKEviction`（core/cache）**：record
（每键环形历史保倒数 K 次）+ evictVictim（倒数第 K 次最早者先逐，
历史不足 K 视作 -∞ 优先——新键未证热度先让路）。K≥1/时间单调
fail-fast；K=1 退化 LRU。落轮 grep 复核无占坑。
