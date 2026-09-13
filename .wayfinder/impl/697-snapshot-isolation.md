# 697 — 快照数据集隔离性深验

**What to build:** SnapshotIsolationDeepTest 三断言（隔离/独立/nextId 续起）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三场景测试
- [x] spec 949 + README 行（欠账累计 926–949）

## Done

验证：`mvn -pl buzhou-core test -Dtest=SnapshotIsolationDeepTest` 全绿。commit 见本轮 `test(core)` 提交。
