# 810 — MCP 工具集轮询提供器读面

**What to build:** DbToolSetProvider 静态四计数（polls/changesDetected/unchangedPolls/pollFailures）+ 嵌套 ToolSetPollStats + stats()/resetForTest() + 变更/无变更/失败/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 四计数落点（轮询入口/变更 fire/无变更成功轮/catch 处）
- [x] ToolSetPollStats 嵌套 record + stats() + resetForTest()
- [x] ToolSetPollStatsTest（变更/无变更/失败/守恒/reset 五测）
- [x] spec 1058 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-mcp -am test -Dtest='ToolSetPollStatsTest'` 全绿 + 既有 DbToolSetProvider 回归绿。commit 见本轮 `feat(mcp)` 提交。
