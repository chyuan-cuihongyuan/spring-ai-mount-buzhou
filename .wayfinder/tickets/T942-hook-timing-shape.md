---
id: T942
title: hook 链耗时观测的形态裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-13
---

## Question

HookChain 同步内联在 Turn 主链路（八个回调面），零耗时观测——一个慢 hook 拖慢所有 Turn 时（TurnStallWatchdog 只见 Turn 级挂死）无法定位是谁。计时面怎么做？开销与刷屏怎么控？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 47 轮 = effort #600 / spec 646 / impl 499）：HookChain 内嵌 per-hook 计时——每回调面 nanoTime 包裹，`ConcurrentHashMap<hookName, Timing>`（LongAdder count/totalNanos + volatile maxNanos）累计；`stats()` 返回不可变快照（hook 名 → count/total/max）。单次超阈值（默认 100ms，常量）WARN 且原子去重（每 hook 每"首次"告一次——TransformingToolCallback fail-open 同款防刷屏）。默认开（纯观测零行为变化；nanoTime+LongAdder 纳秒级开销）。借鉴 Spring Boot Actuator `http.server.requests` per-endpoint 时间分布与 OTel per-span 计时思想。
