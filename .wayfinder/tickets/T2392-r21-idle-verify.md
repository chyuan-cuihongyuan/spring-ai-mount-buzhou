---
id: T2392
title: R21 空闲监控全链接线的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2391
created: 2026-09-15
---

## Question

N 会话第 21 轮：如何验收？

## Resolution

IdleMonitorHolderTest 三断言：全链 sweep（16 分钟后两会话入清单 + 翻转通知 +
直方 total）；Holder.sweepAndRecord 便捷面；SessionFeaturesHook null 构造走
Holder store 且 beforeTurn 喂数 turns=1。回归 Idle/SessionFeatures 三套 12 用例。
