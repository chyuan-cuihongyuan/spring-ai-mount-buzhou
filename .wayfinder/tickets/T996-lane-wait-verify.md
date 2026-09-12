---
id: T996
title: 工具泳道排队时延观测的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T995
created: 2026-09-13
---

## Question

排队真被计量（阻塞耗时计入 total/max）？超时计入 timeouts 且异常语义不变？无竞争路径也累计（微耗时）？零行为回归？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 23 轮）：① 泳道许可占满后并发调用——waitStats.waited 计 1、maxWaitNanos ≥ 阻塞时长、工具结果正确；② 超时路径 → timeouts=1 + IllegalStateException 语义不变 + buzhou.lane.timeout 事件；③ 无竞争直通 → waited 计数微耗时；④ 既有 LaneLimiting 用例零回归。`mvn -pl buzhou-core -am test` 全绿。
