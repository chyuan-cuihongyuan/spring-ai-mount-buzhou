---
id: T1043
title: ToolTimingAggregator 并发正确性压测的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

热路径组件的并发不变量（count=线程数×次数、total 守恒、max=峰值）需压力验证。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 47 轮 = effort #747 / spec 747 / impl 549，测试域补验轮）：N 线程×M 次 record 不变量压测 + 多工具隔离 + windowedMax 一致性。
