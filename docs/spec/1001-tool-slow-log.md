# 1001 — 工具慢调用榜读面

> 来源：J 会话第 2 轮 = effort #1001（[T1453](../../.wayfinder/tickets/T1453-slow-log-shape.md) / [T1454](../../.wayfinder/tickets/T1454-slow-log-verify.md) / impl 754）。借鉴：Redis [SLOWLOG](https://redis.io/docs/latest/commands/slowlog-get/)（slowlog-log-slower-than 阈值 + slowlog-max-len 有界 FIFO 环）。

## Problem Statement

spec 108 timer（P95 分位）与 spec 700 ToolTimingAggregator（per-tool 聚合）都是**聚合面**——「最近哪几次调用慢、慢在哪个工具、是否伴随失败」的单次现场不可见；偶发慢调用（P95 之外的尾部）无现场可查。

## 目标

- 新公共类 `ToolSlowLog`（core.exec，api 面）：
  - `record(toolName, elapsedNanos, failed)`：执行时长**严格大于**阈值才入榜（Redis 口径）；入榜即记 `Entry(toolName, durationMillis, epochMillis, failed)`；
  - 有界 FIFO 环（容量 32——SLOWLOG max-len 同义，超限挤掉最旧；非严格 Top-K 排名）；
  - `entries()`：新→旧只读快照（List.copyOf 不可变）；
  - `configureThreshold(Duration)`：进程级阈值设置（volatile 读，热路径一次比较零担）+ `reset()` 测试注入点；
  - 默认阈值 1000ms（static final 常量，无魔法数字）。
- 接线：`HookedToolCallback` 与 spec 108 timer 同点记录（单次调用至多一次环操作）。

## 兼容性

纯增量读面：timer/counter/aggregator 零变化、无新配置项、默认启用（不达阈值的调用只付一次 volatile 比较）。

## Out of Scope

- 持久化/跨进程慢日志聚合（Redis 有 SLOWLOG RESET/存储后端；本仓止步进程内有界环）。
- 入参快照（SLOWLOG 记 argv——本仓工具入参可能含敏感内容，记名不记参，红线纪律）。
