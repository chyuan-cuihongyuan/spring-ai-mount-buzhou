# 1010 — 轮次时延分位数读面

> 来源：J 会话第 11 轮 = effort #1010（[T1471](../../.wayfinder/tickets/T1471-turn-percentiles-shape.md) / [T1472](../../.wayfinder/tickets/T1472-turn-percentiles-verify.md) / impl 763）。借鉴：numpy [percentile](https://numpy.org/doc/stable/reference/generated/numpy.percentile.html) R-7 默认口径（与 spec 909 同风）；补 spec 191 自己的用户故事。

## Problem Statement

spec 191 用户故事明言「buzhou.turn.duration 的 p95 即『用户体感一轮』基线」，但滚动窗口读面只有 count/avg/max/last——avg 藏尾、max 是单点噪声，p95 缺位：尾延迟（GC 停顿、慢工具拖尾）不可读。

## 目标

- 新公共 record `TurnLatencyPercentiles(long count, double p50Millis, double p95Millis, long maxMillis)`（core.hook，api 面）。
- `TurnTimingHook.percentiles(sessionId)`：对既有 64 样本滚动窗口排序计算（窗口不扩不缩）；空窗/未知会话零值行。
- R-7 线性插值 h=(n−1)·q 抽包级静态纯函数（与 spec 909 同口径）。
- 不改既有 record `TurnStats`（组件变更即破坏构造方——加法不加破损）。

## 兼容性

纯增量读面：beforeTurn/afterTurn/timer/stats 语义零变化；无新配置项。

## Out of Scope

- 自定义分位数集批量读面（p50/p95 已覆盖标准问法；更多分位另行立项）。
- 跨会话聚合分位（会话域读面，聚合属看板域）。
