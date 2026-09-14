# 1068 — 事件总线积压水位读数

**What to build:** EventBackpressureStats 公共静态面（depthWatermark+blockedPushes+Snapshot+reset）+ BufferedEventDispatcher 四点埋点 + 四测。

**Blocked by:** None.

**Status:** done

- [x] EventBackpressureStats（core/session 公共静态面）
- [x] BufferedEventDispatcher 埋点（三处 noteDepth+一处 recordBlockedPush，行为逐位不变）
- [x] EventBackpressureStatsTest 四测（同包直驱分发器）
- [x] spec 1415 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='EventBackpressureStatsTest'` 4/4 绿。
