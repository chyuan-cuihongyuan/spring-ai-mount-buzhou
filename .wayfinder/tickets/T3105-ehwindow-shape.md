---
id: T3105
title: 指数直方图滑窗计数的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

长窗高频事件计数怎么在 O(log N) 空间下带误差界账户化？（spec 2002 / effort #2002 / R3）

## Resolution

**Datar-Indyk 线程安全指数直方图 `ExponentialWindowCounter`（core/metrics）**：
(capacity, first, last) 三元组桶（同容量 ≤2 桶，第 3 个触发最老两桶合并
翻倍、set 回原位保序）+ 整体过期惰性清出 + estimate 全界内计全/跨界计半
+ errorBound=Σ跨界 ceil(cap/2) 自描述 + bucketCount 空间读数。桶存双端
口径是工程改良（经典 EH 只存 last 无法精确判跨界）。
