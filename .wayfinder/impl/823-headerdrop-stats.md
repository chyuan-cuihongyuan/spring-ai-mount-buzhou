# 823 — http_request 受控头丢弃显形

**What to build:** HttpRequestTool 增 headerDrops 第 9 计数（黑名单头命中丢弃处）+ HttpToolStats 尾参追加 + stats()/resetForTest() 同步 + 丢弃显形测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] headerDrops 落点（黑名单 forEach return 处）
- [x] HttpToolStats record 尾参追加（守恒式不变——旁路量）
- [x] HttpToolStatsTest 增丢弃用例（双受控头=2 且请求照常送达）
- [x] spec 1071 + README 行

## Done

验证：`mvn -pl buzhou-tools -am test -Dtest='HttpToolStatsTest'` 全绿 + 回归绿（worktree 隔离）。commit 见本轮 `feat(tools)` 提交。
