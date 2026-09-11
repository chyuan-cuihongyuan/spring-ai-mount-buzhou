# 463 — MCP 每连接并发上限

**What to build:** 注册表 `setPerConnectionConcurrencyLimit`（Entry 信号量装配）+ RefCountingToolCallback 许可层（阻塞可中断、中断失败转文本）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] Entry 并发许可 + 注册表 setter（校验）
- [x] 包装层许可获取/释放（引用计数之内层）
- [x] 4 用例绿（阻塞/跨连接/默认并发/校验）
- [x] spec 610 + README 行

## Done

验证：`mvn -pl buzhou-mcp -am test -Dtest=McpConnectionConcurrencyTest` 绿（4/4）。commit 见本轮 `feat(mcp)` 提交。
