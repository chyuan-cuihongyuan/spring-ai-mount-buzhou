# 494 — 响应缓存 miss 惊群合并

**What to build:** `ResponseCacheCoalescer`（call 路径 miss 合并：leader 独占执行、等待者共享、失败不共享一次性降级、coalescedWaiters 计数）+ advisor opt-in 接线（null = 零变化）+ `response-cache.coalescing` yml 键。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ResponseCacheCoalescer 类 + 失败降级语义
- [x] ResponseCacheAdvisor 构造接线（coalescer 可空）
- [x] ResponseCache record 第 4 槽 coalescing（3 参兼容）
- [x] AutoConfiguration 装配
- [x] 并发合并/失败降级/默认关/yml 绑定 4 用例
- [x] spec 641 + README 行
- [x] 全模块测试绿

## Done

验证：`mvn -pl buzhou-resilience -am test` 绿。commit 见本轮 `feat(resilience)` 提交。
