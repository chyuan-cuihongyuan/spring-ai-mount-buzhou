# 821 — 危险工具守卫判定读面

**What to build:** DangerousToolGuardHook 静态六计数（invocations/disabledSkips/unmatchedSkips/authorizedSkips/exemptedSkips/escalations）+ 嵌套 DangerousToolStats + stats()/resetForTest() + 五路径/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 六计数落点（入口/禁用/未匹配/已授权/豁免/升级）
- [x] DangerousToolStats 嵌套 record + stats() + resetForTest()
- [x] DangerousToolStatsTest（未匹配/升级/守恒/reset 四测）
- [x] spec 1069 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard -am test -Dtest='DangerousToolStatsTest'` 全绿 + 既有 DangerousToolGuard 回归绿。commit 见本轮 `feat(guard)` 提交。
