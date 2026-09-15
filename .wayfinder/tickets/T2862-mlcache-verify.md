---
id: T2862
title: 多级缓存读面的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2861]
created: 2026-09-16
---

## Question]

层级账面在三率/极端/哨兵/畸形四面下正确吗？（spec 1830 / effort #1830 / R31）

## Resolution

**MultiLevelCacheStatsTest 4 用例全绿**（mvn -pl buzhou-resilience test
-Dtest=MultiLevelCacheStatsTest）：60/30/10 三率分读；全 L1/全 L2/全 miss
极端异读；零请求哨兵；负数与四路失恒 fail-fast。

