# 677 — 观测存储水位读面

**What to build:** InMemoryObservabilityStore.watermark() internal 读面 + 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] Watermark record + watermark() 读面
- [x] ObsWatermarkTest（存量一致/上限直通/逐出透传/零行为变化）
- [x] spec 924 + README 行（欠账累计 906–924 十九行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=ObsWatermarkTest` 全绿。commit 见本轮 `feat(core)` 提交。
