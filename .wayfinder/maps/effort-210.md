# Wayfinder Map — Buzhou 跨轮 TTL 工具缓存（effort #210，B 会话第 33 轮）

> B 会话第 33 轮。去重家族补位：轮内 memo（147）只活一轮、在飞合并（139）只压
> 并发——「只读工具结果跨轮复用 TTL 窗」缺位（天气/汇率/目录查询每轮重打）。
> 借鉴 HTTP 响应缓存 max-age 语义。

## Destination

TtlCachingToolCallback（core/exec 装饰器）：key=工具名+argsHash；TTL 窗内
复读直接回缓存值（计数 hit/miss）；过期惰性重执行；容量 LRU 封顶；失败不
缓存。宿主只给「可缓存工具」标注 maxAge。

## Notes

- 号段：B=奇数 spec（本轮 183）；轮次 .wayfinder200+。
- 与响应缓存（53，模型级）/语义缓存（55，相似问）/memo（147，轮内）正交：
> 这是工具级 TTL 面。
- 时效敏感工具不包（契约归声明方——与 retry 的幂等契约同纪律）。

## Decisions so far

- 失败不缓存（可重试信号——与 memo 同口径）。

## Not yet specified

- 负缓存（空结果短 TTL）；JVM 进程外缓存后端。

## Out of scope

- 沿用各轮；相似参数命中（那是语义面）；主动刷新。

## Tickets

- [x] [T555 TtlCachingToolCallback（TTL+LRU+计数）](../tickets/T555-ttl-cache.md)（impl-305）
- [x] [T556 TTL 缓存回归（窗内命中/过期重执行/容量逐出/失败不缓存）](../tickets/T556-ttl-cache-tests.md)（impl-305）
