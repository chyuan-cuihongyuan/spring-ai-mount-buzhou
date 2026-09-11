# 471 — MCP 注解聚入健康面

**What to build:** McpHealth details 增 selfReportedDestructiveToolCount（跨 server 自报 destructive 计数）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 健康聚合 + 分列命名
- [x] 2 用例绿
- [x] spec 618 + README 行

## Done

验证：`mvn -pl buzhou-mcp -am test -Dtest=McpHealthHintsTest` 绿（2/2）。commit 见本轮 `feat(mcp)` 提交。
