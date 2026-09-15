---
id: T2801
title: O 系对账门的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question

150 轮 O 会话的工件链对账门用什么形状防号段漂移？（spec 1800 / effort #1800 / R1）

## Resolution

**数值区间号段 + 四面互证四断言**（starter 测试
OSession1800LedgerAuditTest）：spec 1800–1949 区间过滤（跨双前缀不用正则）；
票对公式 T2801+2(N−1800)/+1；impl 1401+(N−1800)；README 每 spec 号必现；
起点 1800 严格递增。LSession1700LedgerAuditTest 预防式对账同款，范围自扩展
（后续轮落地自动纳入）。
