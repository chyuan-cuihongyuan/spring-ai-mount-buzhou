---
id: T1180
title: 泄漏疑似对象聚合器验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1179]
created: 2026-09-13
---

## Question

聚合/稳键/溢出如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 40 轮 = effort #839）：LeakSuspectAggregatorTest 4 例——同键计数+最大龄 500+排行/长描述截 64 去重/溢出桶 32+1+truncated/脏报告三形态+空真。
