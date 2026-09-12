# 491 — 熔断时间窗生效读面

**What to build:** ResilienceStats.circuitTimeWindowMs（configure 写入 + details 直读）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] stats 字段/setter + configure 接线
- [x] 1 用例双态 + 全模块零回归
- [x] spec 638 + README 行

## Done

验证：`mvn -pl buzhou-resilience -am test` 绿。commit 见本轮 `feat(resilience)` 提交。
