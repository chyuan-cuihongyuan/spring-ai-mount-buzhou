# 747 — ToolTimingAggregator 并发正确性压测

> 来源：G 会话第 47 轮 = effort #747（spec 700 补验）/ [T1043](../../.wayfinder/tickets/T1043-tool-timing-concurrency-shape.md) / [T1044](../../.wayfinder/tickets/T1044-tool-timing-concurrency-verify.md) / impl 549。

## 背景

ToolTimingAggregator 是热路径组件（每工具调用都 record）——并发正确性（count=线程数、total=各线程值之和、max=峰值）需压力验证。

## 目标（测试域补验轮）

- N 线程 × M 次 record 同工具：count == N×M、total == 各线程值和、max == 理论峰值；
- 多工具并发互不串账；
- windowedMax 与生命周期 max 一致（同窗）。

## 兼容性

纯测试域增量。
