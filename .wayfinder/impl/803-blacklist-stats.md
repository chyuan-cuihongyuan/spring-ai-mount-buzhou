# 803 — 命令黑名单拦截判定读面

**What to build:** CommandBlacklist 静态三计数（checks/matched/allowed）+ 嵌套 CommandBlacklistStats + stats()/resetForTest() + 命中/放行/空白短路/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数落点（入口/true/false；空白短路归 allowed 口径）
- [x] CommandBlacklistStats 嵌套 record + stats() + resetForTest()
- [x] CommandBlacklistStatsTest（危险命令命中/安全命令放行/空白/守恒/reset 五测）
- [x] spec 1051 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-tools -am test -Dtest='CommandBlacklistStatsTest'` 全绿 + 既有 CommandBlacklist/RunCommandTool 回归绿。commit 见本轮 `feat(tools)` 提交。
