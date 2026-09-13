# 788 — MCP properties 装配解析统计读面

**What to build:** PropertiesToolSetProvider 静态三计数（servers/bindings/bindingsSkipped）+ 嵌套 PropertiesParseStats + parseStats()/resetForTest() + 解析统计测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数埋点（server/bindings/skipped 三路）
- [x] PropertiesParseStats 嵌套 record + parseStats()/resetForTest()
- [x] McpParseStatsTest（计数/fail-fast 回归/非 Map 项跳过显形/归零）
- [x] spec 1036 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-mcp test -Dtest='McpParseStatsTest'` 全绿 + 既有装配回归绿。commit 见本轮 `feat(mcp)` 提交。
