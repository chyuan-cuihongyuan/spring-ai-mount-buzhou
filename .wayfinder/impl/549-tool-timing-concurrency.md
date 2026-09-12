# 549 — ToolTimingAggregator 并发正确性压测

**What to build:** 并发压测用例（count/total/max 不变量）+ 多工具隔离 + windowedMax 一致性（测试域轮）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 压测用例
- [x] spec 747 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `test(core)` 提交。
