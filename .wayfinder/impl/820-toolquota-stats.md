# 820 — 工具配额消耗读面

**What to build:** ToolQuotaHook 静态五计数（calls/allowed/quotaBlocks/unmanagedSkips + excludedTokens 旁路）+ 嵌套 ToolQuotaStats + stats()/resetForTest() + 消耗/拒绝/豁免/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 五计数落点（入口/放行/超限/未管辖/坏值修正）
- [x] ToolQuotaStats 嵌套 record + stats() + resetForTest()
- [x] ToolQuotaStatsTest（允许/拒绝/未配置/守恒/reset 五测）
- [x] spec 1068 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard -am test -Dtest='ToolQuotaStatsTest'` 全绿 + 既有 ToolQuotaHook 回归绿。commit 见本轮 `feat(guard)` 提交。
