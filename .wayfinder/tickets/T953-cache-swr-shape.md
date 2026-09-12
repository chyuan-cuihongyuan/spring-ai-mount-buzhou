---
id: T953
title: 缓存 stale-while-revalidate 的形态裁决（响应缓存容量上限 ruled-out 后顺延）
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

原列主题「响应缓存容量上限+逐出计数」经缺口核查已被 spec 53 §D 完整实现（maxEntries+evictions+hitRate）——ruled-out 顺延。SWR（过期先回旧值再后台刷新）落点在哪？并发刷新惊群与刷新失败语义怎么定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 2 轮 = effort #701 / spec 701 / impl 504）：落点 = **TtlCachingToolCallback**（spec 183 跨轮工具缓存——刷新=重放工具调用 delegate.call，天然可重放；advisor 层重放原始请求需存 prompt，复杂不值）。语义：`wrap` 新增 swrGrace 参数工厂（默认 0=关，现行为逐字节不变）——过期后 grace 窗内命中：**同步回 stale 值**（调用者零等待）+ 虚拟线程后台重放刷新；grace 窗外：硬过期（现行为）。刷新单飞：锁内 in-flight 集合，N 个并发 stale 命中只触发一次刷新（golang singleflight / nginx proxy_cache_lock 同款思想）；刷新失败：保留旧 stale 值 + refreshFailures 计数 + WARN（stale 可用性优先——nginx `proxy_cache_use_stale updating error` 语义）。观测：staleServed/refreshFailures getter（Stats record 不动——加构造分量是破坏性变更）。借鉴 nginx `proxy_cache_use_stale` + guava refreshAfterWrite + RFC 5861 stale-while-revalidate。
