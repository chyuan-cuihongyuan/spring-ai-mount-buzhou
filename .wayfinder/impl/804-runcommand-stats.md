# 804 — run_command 执行结果分布读面

**What to build:** RunCommandTool 静态九计数（attempts/exits/canceled/timeouts + blank/blacklist/workdir/timeoutParam/failures 五拒绝桶）+ 嵌套 RunCommandStats + stats()/resetForTest() + 结局分布/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [ ] 九计数落点（call 四拒绝点 + execute 取消/超时点 + exits 送达点 + catch 兜底）
- [ ] RunCommandStats 嵌套 record + totalRejects 派生 + stats() + resetForTest()
- [ ] RunCommandStatsTest（正常/非零 exit/空命令/黑名单/坏 workdir/坏 timeout/超时/守恒/reset 八测）
- [ ] spec 1052 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-tools -am test -Dtest='RunCommandStatsTest'` 全绿 + 既有 RunCommandToolTest 回归绿（worktree 隔离）。commit 见本轮 `feat(tools)` 提交。
