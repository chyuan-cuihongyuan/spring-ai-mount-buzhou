---
id: T2421
title: R36 失败项重跑的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2420
created: 2026-09-15
---

## Question

N 会话第 36 轮：rerun-failed 做独立 API 还是 run 子集参数化？

## Resolution

选 **run 子集参数化（onlyItemIds）**。独立 API 重复 run 的执行/落盘/事件
链路；子集过滤一行接入且口径天然一致（total=子集数诚实反映本次跑的范围）。
flaky 区分（重跑过=flaky）由宿主对比两轮结果得出——编排面不侵入 runner。
