# 828 — Runaway 预算 hook 判定读面

**What to build:** RunawayHook 静态四计数（invocations/blocked/allowed/disabledSkips）+ 嵌套 RunawayStats + stats()/resetForTest() + 放行/硬顶/禁用/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 四计数落点（入口/三硬顶出口合桶/正常放行/禁用）
- [x] RunawayStats 嵌套 record + stats() + resetForTest()
- [x] RunawayStatsTest（放行/硬顶/禁用/守恒/reset 五测）
- [x] spec 1076 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='RunawayStatsTest'` 全绿 + 既有 RunawayHook 回归绿。commit 见本轮 `feat(core)` 提交。
