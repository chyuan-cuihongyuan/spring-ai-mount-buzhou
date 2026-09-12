---
id: T1024
title: span 状态分布读数的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

span 状态分布与 RUNNING 残留（泄漏信号）无读数面。做纯函数原语吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 13 轮 = effort #712 / spec 712 / impl 612）：`SpanStatusDistribution.of(List<SpanRecord>)` 纯函数——(kind,status) 聚合字典序+runningResidue（未关闭 span 泄漏信号）+errorRate（ERROR/total，total=0 诚实 0.0）；状态字符串不假设闭集（前向兼容）；纯读数（告警归 312）。OTel span status 思想。
