# 673 — webhook 限流器余量快照读面

**What to build:** WebhookRateLimiter.snapshot（同锁强一致余量/容量/流速/deferred 投影）+ 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] snapshot() + Snapshot record
- [x] RatelimitSnapshotTest（满桶起步/消耗递减/deferred 透传/既有零回归）
- [x] spec 920 + README 行（欠账累计 906–920 十五行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=RatelimitSnapshotTest` 全绿。commit 见本轮 `feat(core)` 提交。
