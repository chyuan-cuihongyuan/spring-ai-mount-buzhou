# 813 — Dashboard HTTP 状态分布读面

**What to build:** DashboardHttpServer 静态八计数（requests/ok + auth/bad/notFound/tooLarge/unimplemented/serverErrors 六结局桶）+ 嵌套 DashboardHttpStats + stats()/resetForTest() + 状态分布/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 八计数落点（route 入口/各 catch/正常完成）
- [x] DashboardHttpStats 嵌套 record + stats() + resetForTest()
- [x] DashboardHttpStatsTest（成功/401/400/404/守恒/reset 六测）
- [x] spec 1061 + README 行（internal 类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-observe-dashboard -am test -Dtest='DashboardHttpStatsTest'` 全绿 + 既有 Dashboard 回归绿。commit 见本轮 `feat(dashboard)` 提交。
