# Spec 183 — 跨轮 TTL 工具缓存（effort #210）

> wayfinder map：`.wayfinder/maps/effort-210.md`（T555–T556）。借鉴：HTTP 响应缓存
> max-age——只读查询结果的跨轮复用窗。

## Problem Statement

只读查询型工具（天气/汇率/目录/字典）的答案在分钟级内不变，但每轮都真实
执行：轮内 memo（147）救不了下一轮，响应缓存（53）在模型面不认工具语义——
工具级 TTL 缓存缺位，下游 QPS 与轮延迟双输。

## Solution

`TtlCachingToolCallback`（core/exec，装饰器）：

- `wrap(callback, maxAge, maxEntries)`——key = 工具名 + argsHash（与事件日志
  幂等键同口径）；TTL 窗内复读直接回缓存值（引用一致）；过期惰性重执行刷新。
- 容量 LRU 封顶（逐出最久未用）；<b>失败不缓存</b>（异常上抛不入表——与
  memo 同口径，可重试信号）。
- 计数 hit / miss / evicted；`stats()` 观测。
- 契约：只包时效钝感的只读工具（时效敏感/写工具不包——归声明方）。

## User Stories

1. 作为宿主，目录查询工具包 5 分钟 TTL——同参复读零下游执行，轮延迟直降。
2. 作为运维，hit/miss 比即「复读率×时效窗」收益；evicted 高就调 maxEntries。
3. 作为模型，缓存值引用一致——同窗内答案自洽不漂移。

## Implementation Decisions

- Clock 注入（TTL 断言确定性）；单实例（进程内——跨实例归 Redis 缓存族）。

## Testing Decimals

- 窗内命中零重执行；过期重执行刷新窗；LRU 逐出最久未用；失败不缓存再调重试；
  异参各缓存；计数正确。

## Out of Scope

- 负缓存；主动刷新；跨实例共享。

## Further Notes

- 去重家族五员：响应缓存（53）/ 语义缓存（55）/ 在飞合并（139）/ 轮内 memo
  （147）/ 工具 TTL（本轮）。
