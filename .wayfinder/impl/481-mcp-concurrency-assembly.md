# 481 — MCP 每连接并发上限 yml 装配

**What to build:** BuzhouMcpProperties 扩组件 + Builder 透传 + 注册表接线。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 属性/Builder/接线 + @ConstructorBinding 坑修复
- [x] 3 用例绿 + mcp 全模块 50/50 零回归
- [x] spec 628 + README 行

## Done

验证：`mvn -pl buzhou-mcp -am test` 绿。commit 见本轮 `feat(mcp)` 提交。
