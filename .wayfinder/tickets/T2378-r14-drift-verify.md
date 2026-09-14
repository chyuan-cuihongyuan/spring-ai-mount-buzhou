---
id: T2378
title: R14 工具目录漂移看门狗接线的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2377
created: 2026-09-15
---

## Question

N 会话第 14 轮：如何验收？

## Resolution

CatalogDriftWiringTest：三会话序列（[a,b]→[a,c]→[a,c]）真实 spawn——首会话零
事件建基线、次会话一漂移事件（added/removed 明细）、第三会话稳定零事件；
Holder 直喂面（snapshot 后 check 得 diff.added）。既有 CatalogDriftWatcherTest
5 用例零回归。
