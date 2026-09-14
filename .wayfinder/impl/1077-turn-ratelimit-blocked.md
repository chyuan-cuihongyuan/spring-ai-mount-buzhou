# 1077 — 轮次限速 per-key 拒绝榜

**What to build:** TurnRateLimitHook 增量（blockedByKeys 表 256 折叠+blockedSnapshot+resetBlockedForTest）+ 三测。

**Blocked by:** None.

**Status:** done

- [x] TurnRateLimitHook 拒绝表（beforeTurn 拦截路径单点记账，放行零动作）
- [x] TurnRateLimitBlockedSnapshotTest 三测
- [x] spec 1424 + README 行（既有类增量——快照面不变）

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='TurnRateLimitBlockedSnapshotTest,TurnRateLimitHookTest'` 7/7 绿。
