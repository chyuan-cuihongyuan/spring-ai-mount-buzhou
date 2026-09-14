# 1086 — 工具循环打断分布读面

**What to build:** ToolLoopBreakerHook 增量（brokenByTool 表 256 折叠+brokenTotal+maxRunObserved+resetBrokenForTest）+ 三测。

**Blocked by:** None.

**Status:** done

- [x] ToolLoopBreakerHook 打断路径埋点（放行零动作）
- [x] ToolLoopBreakerBreakStatsTest 三测
- [x] spec 1433 + README 行（既有类增量——快照面不变）

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='ToolLoopBreakerBreakStatsTest'` 3/3 绿。
