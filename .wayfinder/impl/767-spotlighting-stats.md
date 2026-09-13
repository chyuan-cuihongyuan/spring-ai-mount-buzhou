# 767 — Spotlighting 应用与损坏计数读面

**What to build:** SpotlightingStats record + 进程级三计数（wrapped/unwrapped/malformed 守恒）+ stats()/resetForTest + 双轨测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数埋点（unwrap 入口/成功/畸形三路）
- [x] SpotlightingStats record + stats()/resetForTest()
- [x] SpotlightingStatsTest（往返/明文不计/畸形/守恒/归零）
- [x] spec 1014 + README 行 + API 快照与 api-surface.md 增行

## Done

验证：`mvn -pl buzhou-core test -Dtest='SpotlightingStatsTest,SpotlightingTest'` 全绿。commit 见本轮 `feat(core)` 提交。
