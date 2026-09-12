# 504 — 缓存 stale-while-revalidate（跨轮工具缓存 SWR）

**What to build:** TtlCachingToolCallback 新增 swrGrace opt-in 工厂——过期后 grace 窗内同步回 stale 值 + 虚拟线程后台单飞刷新，刷新失败保旧值；观测 staleServed/refreshFailures。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] swrGrace 工厂重载（默认 wrap=0 关，现行为不变）
- [x] grace 窗 stale 回程 + 虚拟线程后台刷新
- [x] in-flight 单飞（并发 N 次命中只刷一次）
- [x] 刷新失败保旧值 + WARN + refreshFailures
- [x] staleServed/refreshFailures getter（Stats record 不动）
- [x] Clock 注入五组用例 + 既有零回归
- [x] spec 701 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
