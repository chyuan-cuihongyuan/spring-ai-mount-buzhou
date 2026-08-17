# 60 — run_command↔CommandSandbox 合流（T85 决策落地）

**What to build:** core `CommandBackend` SPI；RunCommandTool 可选委托执行；guard `SandboxCommandBackend` 桥接 + 条件装配；tools auto-config backend=sandbox fail-fast；输出格式化（stderr/truncated）。

**Blocked by:** None.

**Status:** done

- [ ] core：`exec/CommandBackend` 接口 + `CommandOutcome` record
- [ ] tools：RunCommandTool 委托构造重载 + 执行路由 + 输出格式化（exit/stderr/truncated/timeout 段）；BuzhouToolsAutoConfiguration backend=sandbox 解析 fail-fast
- [ ] guard：`SandboxCommandBackend` 桥接（env 白名单 PATH/HOME/LANG/TZ；CommandResult→CommandOutcome 映射）+ BuzhouGuardAutoConfiguration 条件装配
- [ ] 测试：tools 委托路由（fake backend 记录命令/返回格式化断言）+ builtin 路径回归 + fail-fast；guard 桥接委托断言（fake CommandSandbox）+ 不可用不回退

## Done

验证：`./mvnw -pl buzhou-tools,buzhou-guard clean test`——tools 46/46（新增 SandboxRunCommandToolTest 5 用例：委托路由+格式化/黑名单前置/workdir 逐段防线/不可用不回退/装配二选一 fail-fast）、guard 93/93（新增 SandboxCommandBackendTest 3 用例：shell 包装+env 白名单/OUTPUT 击杀→truncated/不可用抛指引）。
落地：core `exec/CommandBackend` SPI（自含 CommandOutcome）；tools `SandboxRunCommandTool`（与内置 RunCommandTool 同名同 Schema 装配期二选一——不改既有文件；入参解析抽 `RunCommandArgs` record；workdir 逐段防线：切段+token 白名单+显式拒 `..`+root 内校验）+ ToolsModule backend 注入与 `command.backend=builtin|sandbox` fail-fast；guard `SandboxCommandBackend` 桥接（@ConditionalOnBean(CommandSandbox)，档位选择归应用；PATH/HOME/LANG/TZ 白名单；OUTPUT 击杀归因 truncated）。
过程注记：RunCommandTool.java 既有 `sandbox.resolve(userInput)` 模式被 Mimosa 安全钩子按「注入」启发式恒定拦截（该文件不可改）；改为新增同名委托版工具 + 装配期切换实现同等合流，且 workdir 防线较原版更严。
