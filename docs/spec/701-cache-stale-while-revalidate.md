# 701 — 缓存 stale-while-revalidate（跨轮工具缓存 SWR）

> 来源：G 会话第 2 轮 = effort #701（借鉴 nginx `proxy_cache_use_stale` / guava refreshAfterWrite / RFC 5861）/ [T953](../../.wayfinder/tickets/T953-cache-swr-shape.md) / [T954](../../.wayfinder/tickets/T954-cache-swr-verify.md) / impl 504。
> 选题注记：原列主题「响应缓存容量上限+逐出计数」缺口核查已被 spec 53 §D 覆盖（maxEntries + evictions + hitRate）——ruled-out 顺延。

## 背景

TtlCachingToolCallback（spec 183）过期即重执行：时效钝感只读工具（配置查询、目录列举）在 TTL 边界后第一次调用要**同步吃满工具耗时**——即便旧值依然可接受。nginx 的洞察：旧值先回、后台刷新（stale-while-revalidate），边界调用延迟归零。

## 目标

- `wrap` 新增 swrGrace 参数工厂：过期后 grace 窗内命中——**同步回 stale 值**（调用者零等待）+ 虚拟线程后台重放工具刷新缓存；grace 窗外——硬过期（现行为）。
- 刷新单飞：锁内 in-flight 集合，N 个并发 stale 命中只触发一次刷新（nginx proxy_cache_lock / golang singleflight 思想）。
- 刷新失败：保留旧 stale 值 + WARN + 计数（`proxy_cache_use_stale updating error`——stale 可用性优先，绝不回 null）。
- 观测：`staleServed()` / `refreshFailures()` getter（既有 Stats record 三分量不动——加构造分量是破坏性变更）。
- 默认 `wrap`（grace=0）现行为逐字节不变。

## 非目标

不做 ResponseCacheAdvisor 层 SWR（重放原始请求需存 prompt——复杂不值，票 T953 裁决）；不做主动定时刷新（惰性触发即可）。

## 测试

Clock 注入：① TTL 后 grace 内调用立即回旧值 + 后台刷新落表后命中新值；② 刷新异常保旧值 + refreshFailures；③ grace 外硬过期现行为；④ 并发 N 线程同 key stale 命中 delegate 重放恰 1 次；⑤ 默认 wrap 既有用例零回归。

## 兼容性

opt-in 纯增量；grace=0 路径逐字节不变。
