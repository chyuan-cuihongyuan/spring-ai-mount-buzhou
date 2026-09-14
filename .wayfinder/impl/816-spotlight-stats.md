# 816 — 读侧 Spotlighting 包裹判定读面

**What to build:** SpotlightHook 静态五计数（invocations/wrapped/alreadyWrappedSkips/noticeSkips/errorSkips）+ 嵌套 SpotlightStats + stats()/resetForTest() + 四路径/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 五计数落点（入口/包裹/幂等跳/告示跳/error 跳）
- [x] SpotlightStats 嵌套 record + stats() + resetForTest()
- [x] SpotlightStatsTest（包裹/幂等/告示/error/守恒/reset 六测）
- [x] spec 1064 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard -am test -Dtest='SpotlightStatsTest'` 全绿 + 既有 SpotlightHook 回归绿。commit 见本轮 `feat(guard)` 提交。
