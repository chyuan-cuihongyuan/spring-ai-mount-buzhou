---
id: T1106
title: 指标新鲜度审计验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1105]
created: 2026-09-13
---

## Question

写入刷新/陈旧判定/封顶边界如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 3 轮 = effort #802）：MetricFreshnessTrackerTest 6 例——写入刷新+委托透传+刷新不新增/陈旧年龄降序（10s>5s>fresh 过滤）/gauge 不追踪/名字封顶 512 精确+truncated/空名+null 忽略+清单封顶 64/fail-fast 三参。封顶测试首跑抓获 CAS 反转溢出 bug（512→513）——修复后全绿。
