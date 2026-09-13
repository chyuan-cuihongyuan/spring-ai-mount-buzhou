# 781 — 模型窗口解析分布读面

**What to build:** TableContextWindowResolver 三路命中计数 + resolvedWindows 有界快照 + 嵌套 WindowResolutionStats + stats() + 三路分布测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三路命中计数（override/builtIn/fallback）
- [x] resolvedWindows 有界快照（不可变）
- [x] WindowResolutionStats 嵌套 record + stats()
- [x] TableContextWindowResolverStatsTest（三路/守恒/快照不可变/回退值）
- [x] spec 1028 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-core test -Dtest='TableContextWindowResolverStatsTest'` 全绿 + 既有窗口解析回归绿。commit 见本轮 `feat(core)` 提交。
