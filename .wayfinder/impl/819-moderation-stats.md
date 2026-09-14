# 819 — 内容安全词表双缝判定读面

**What to build:** ContentModerationHook 静态五计数（invocations/blocked/masked/cleanSkips/nullSkips）+ 嵌套 ModerationStats + stats()/resetForTest() + 双缝动作/跳过/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 五计数落点（双缝入口/BLOCK/MASK/无命中/null 跳过）
- [x] ModerationStats 嵌套 record + stats() + resetForTest()
- [x] ModerationStatsTest（BLOCK/MASK/无命中/null/守恒/reset 六测）
- [x] spec 1067 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard -am test -Dtest='ModerationStatsTest'` 全绿 + 既有 ContentModerationHook 回归绿。commit 见本轮 `feat(guard)` 提交。
