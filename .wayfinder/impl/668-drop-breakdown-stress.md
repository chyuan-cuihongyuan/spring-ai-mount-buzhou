# 668 — EventDropBreakdown 并发压测

**What to build:** EventDropBreakdownConcurrencyTest（4000 并发守恒 + 分类值域封闭 + 多实例隔离）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 4000 并发 enqueue 压测（守恒不变量精确断言）
- [x] 多分发器隔离场景
- [x] spec 915 + README 行（欠账累计 906–915 十行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=EventDropBreakdownConcurrencyTest` 全绿。commit 见本轮 `test(core)` 提交。
