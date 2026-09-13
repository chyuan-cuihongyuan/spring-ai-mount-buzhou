---
id: T1184
title: 会话空闲时长直方验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1183]
created: 2026-09-13
---

## Question

落桶/边界恰达/标签如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 42 轮 = effort #841）：IdleDurationHistogramTest 4 例——五桶落位含恰达/自定义+负值忽略/标签四例/全桶快照。humanize 毫秒档统一修正。
