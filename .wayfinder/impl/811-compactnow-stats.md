# 811 — compact_now 手动压缩判定读面

**What to build:** CompactNowTool 静态五计数（calls/successes/skippeds/failures/unboundRejects）+ 嵌套 CompactNowStats + stats()/resetForTest() + 四路径/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 五计数落点（入口/完成/跳过/失败/未绑定）
- [x] CompactNowStats 嵌套 record + stats() + resetForTest()
- [x] CompactNowStatsTest（成功/跳过/未绑定/守恒/reset 等测）
- [x] spec 1059 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-memory -am test -Dtest='CompactNowStatsTest'` 全绿 + 既有 CompactNowTool 回归绿。commit 见本轮 `feat(memory)` 提交。
