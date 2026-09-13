---
id: T1292
title: webhook 限流器余量快照读面的验证
type: task
status: closed
assignee: zcode-i
blocked-by: T1291
created: 2026-09-14
---

## Question

满桶起步 tokens==capacity？消耗后余量递减？refill 时点修正生效？deferred 计数透传？既有 tryAcquire 零回归？
