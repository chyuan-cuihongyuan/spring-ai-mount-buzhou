# 527 — MCP keepalive yml 装配

**What to build:** Builder.keepalive + fromYml keepalive-interval 键 + McpModule→注册表 9 参直通；缺省零变化。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] Builder fluent + fromYml 键解析 + 构造直通
- [x] 声明生效/缺省零回归用例
- [x] spec 724 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-mcp -am test` 绿。commit 见本轮 `feat(mcp)` 提交。
