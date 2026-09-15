---
id: T2849
title: SWR 策略的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

缓存过期的中间态（回旧值+异步刷新）怎么判？（spec 1824 / effort #1824 / R25）

## Resolution

**HTTP stale-while-revalidate/CDN 思想纯判态 `StaleWhileRevalidatePolicy`
（buzhou-resilience/cache）**：serving 三态 FRESH/STALE（旧值+异步刷新）/
EXPIRED（同步回源），边界归属显式（达 fresh 进陈旧、达总寿过期）+
staleness 读数（0=新鲜钳零、(0,1)=窗内进度、≥1=过期；fresh=0 -1 哨兵）；
零陈旧窗退化纯 TTL。初版负陈旧度与哨兵撞值改钳 0（设计期自查）。

