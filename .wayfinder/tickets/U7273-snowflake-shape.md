---
id: U7273
title: U 会话 U37 SnowflakeIdGenerator 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

分布式发号怎么防重号？（spec 7036 / effort #7036 / U37）

## Resolution

**SnowflakeIdGenerator（core/concurrent）**：64 位分段；同毫秒序列自增溢出自旋；时钟回退 fail-fast；步进时钟注入。
