# 641 — 响应缓存 miss 惊群合并（singleflight）

> 来源：F 会话第 42 轮 = effort #600（spec 53 的并发面补全）/ [T932](../../.wayfinder/tickets/T932-cache-stampede-shape.md) / [T933](../../.wayfinder/tickets/T933-cache-stampede-verify.md) / impl 494。借鉴：golang x/sync/singleflight、nginx `proxy_cache_lock`。

## 背景

精确响应缓存只保护「二次调用」——首个 miss 窗口内并发相同请求（model+messages+options 同 key）各自打模型：eval 并行集、模板化批处理、重试风暴场景下 = cache stampede（缓存惊群），N 路并发 = N 次模型调用 + N 倍成本。

## 目标

`ResponseCacheCoalescer`（resilience.cache）+ `buzhou.resilience.response-cache.coalescing`（默认 false，opt-in）：

- **call 路径** miss 后合并：同 key 并发只放一路（leader）打模型，其余等待者共享其结果（leader 完成后各等待者从缓存语义等价路径拿到同一终态响应包装）。
- **失败不共享**：leader 异常时等待者各自直调（一次性降级——nginx proxy_cache_lock 语义：fetch 失败等待者重新竞争；不比无合并现状差）。
- **等待与 leader 同生共死**：无独立等待超时——模型调用超时/deadline 兜底（advisor 链序 +450 在 resilience +700 外层，nextCall 穿入 deadline 保护；等待者挂死形式共享 leader 挂死，非新增风险）。
- **可观测**：`coalescedWaiters()` 计数（AtomicLong）——合并省下的模型调用数直接可读。
- in-flight map 条目 leader 终态即清（两参 remove 防误删新代 entry），无泄漏。

## 非目标

流式不合并（adviseStream 原样）：等待者要实时 chunk，共享聚合 Flux 语义微妙（首个 chunk 前的等待者尚可、中途加入者无正确语义），v1 诚实不做。语义缓存不合并（桶键非精确键，合并键语义待议）。

## 测试

并发合并（模型调用 = 1、同结果、waiters 计数）/ leader 失败降级（模型调用 = N）/ 默认关零变化 / yml opt-in 绑定 + 全模块零回归。

## 兼容性

默认关零行为变化；`ResponseCache` record 第 4 槽 coalescing（3 参构造兼容）；advisor 新构造参数 null = 既有路径。
