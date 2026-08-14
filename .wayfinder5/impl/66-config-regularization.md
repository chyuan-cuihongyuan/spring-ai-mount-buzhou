# 66 — skills/mcp 配置正规化 + 全仓 verify 里程碑（T91 决策落地）

**What to build:** BuzhouSkillsProperties / BuzhouMcpProperties record + auto-config 注入改造 + map 契约文档化 + 全仓 clean verify。

**Blocked by:** None.

**Status:** done

- [ ] `BuzhouSkillsProperties`（prefix buzhou.skills：enabled 默认 true / dbEnabled 默认 true）
- [ ] `BuzhouMcpProperties`（prefix buzhou.mcp：enabled 默认 true / dangerousToolPatterns / shutdownBudget 默认 35s；负预算 fail-fast）
- [ ] 两 auto-config 改注入 properties（env.getProperty 散键清零）
- [ ] 既有模块测试回归 + 全仓 `mvn clean verify` 里程碑

## Done

验证：`./mvnw -pl buzhou-skills,buzhou-mcp clean test`——skills 62/62、mcp 35/35 绿；全仓 `mvn clean verify` 里程碑（见下）。
落地：`BuzhouSkillsProperties`（enabled/dbEnabled 归一）+ `BuzhouMcpProperties`（enabled/dangerousToolPatterns/shutdownBudget 归一 + 负预算 fail-fast）+ 两 auto-config 注入改造（散键直读清零）；map 契约 by-design 文档化（spec 21）。
**里程碑重要发现**：全仓 clean verify 揭露 impl-60 遗留假绿——ToolsModule 缺 `SandboxRunCommandTool` import（增量编译残留掩盖）+ 测试缺嵌套类型 import + macOS /var 符号链接路径断言偏差。已修复：tools clean test 51/51 真实绿。此后以 **clean test / clean verify 为唯一可信验证**（增量编译在本机不可信）。
