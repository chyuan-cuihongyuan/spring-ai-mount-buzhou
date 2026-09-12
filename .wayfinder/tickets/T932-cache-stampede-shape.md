---
id: T932
title: 响应缓存 miss 惊群合并的形态裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-13
---

## Question

ResponseCacheAdvisor miss 后并发相同 cacheKey 各自打模型（eval 并行集/模板化批处理/重试风暴场景 = cache stampede）。合并吗？失败语义与流式怎么办？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 42 轮 = effort #600 / spec 641 / impl 494）：opt-in 合并（`buzhou.resilience.response-cache.coalescing`，默认 false 零变化）。`ResponseCacheCoalescer`：call 路径 miss 后 leader 独占执行、同 key 等待者共享结果（golang x/sync/singleflight + nginx proxy_cache_lock 思想）。**失败不共享**——leader 异常时等待者各自直调（一次性降级，不比现状差）；等待与 leader 同生共死（模型超时/deadline 兜底，无独立等待超时）。流式不合并（非目标：等待者要实时 chunk，共享聚合 Flux 语义微妙）。`coalescedWaiters()` 计数——合并省下的模型调用数直接可读。
