---
id: T3121
title: 判定决策缓存的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

护栏重复判定的短路缓存怎么定语义？（spec 2010 / effort #2010 / R11）

## Resolution

**OPA/Cedar 线程安全决策缓存 `DecisionCache<K,V>`（buzhou-guard
decision）**：TTL 内命中短路+过期惰性清除（expirations 与 misses 分计
——过期非未见过）+put 覆盖刷新时间戳+超容 LRU 驱逐（accessOrder）+
invalidate 显式失效（策略热更新精准失效）+四计数/size/hitRate 有效性
证+时钟回拨宽进显式文档。
