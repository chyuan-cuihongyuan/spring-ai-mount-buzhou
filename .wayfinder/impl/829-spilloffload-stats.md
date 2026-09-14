# 829 — Spill 溢出 hook 判定读面

**What to build:** SpillOffloadHook 静态六计数（invocations/durableSkips/errorSkips/cleanInline/offloaded/refrains）+ 嵌套 SpillOffloadStats + stats()/resetForTest() + 五结局/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 六计数落点（入口/durable/error/内联/溢出/refrain）
- [x] SpillOffloadStats 嵌套 record + stats() + resetForTest()
- [x] SpillOffloadStatsTest（溢出/内联/error/守恒/reset 等测）
- [x] spec 1077 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-spill -am test -Dtest='SpillOffloadStatsTest'` 全绿 + 既有 SpillOffloadHook 回归绿。commit 见本轮 `feat(spill)` 提交。
