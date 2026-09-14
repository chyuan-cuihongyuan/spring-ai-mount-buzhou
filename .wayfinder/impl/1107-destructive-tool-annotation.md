# 1107 — BuzhouTool destructive 风险注解（M 系 R5）

**What to build:** @BuzhouTool 加 destructive() 默认 false；内置破坏性工具标注；ToolsModule 危险名单注解驱动化（行为等价）。

**Blocked by:** T2259 / T2260（同轮 shape+verify）。

**Status:** done

- [x] 注解成员 + Javadoc（MCP destructiveHint 思想）
- [x] write_file / run_command / SandboxRunCommandTool / http_request 标注
- [x] ToolsModule 扫描注解生成名单（删三处手工登记）
- [x] 既有断言零变化 + 自定义 destructive 工具自动入名单用例

## Done

验证：tools 模块测试绿。commit 见本轮 feat 提交。
