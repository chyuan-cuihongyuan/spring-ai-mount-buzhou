# 486 — resilience 观测/装配补验

**What to build:** panicActivations() getter（与指标同源）+ circuit.time-window yml 绑定用例。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] getter + 计数断言
- [x] 绑定用例 + 全模块 241/241 零回归
- [x] spec 633 + README 行

## Done

验证：`mvn -pl buzhou-resilience -am test` 绿。commit 见本轮 `feat(resilience)` 提交。
