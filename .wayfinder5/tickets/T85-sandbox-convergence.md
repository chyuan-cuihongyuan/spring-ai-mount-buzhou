---
Type: task
Status: closed
assignee: zcode
blocked-by:
---
## Question

run_command 与 guard CommandSandbox 双轨如何合流？现状：RunCommandTool 自建 FileSandbox+CommandBlacklist+裸 ProcessBuilder；guard 的 Deno/E2E/Firecracker CommandSandbox 实现与 tools 零互引，危险路径跑的是较弱的一套。约束：依赖图物理无环、tools 不依赖 guard。决策点：port 接口放哪（core SPI 新接口 CommandExecutionBackend？guard 实现+桥接注册，tools 运行时 Optional 注入）、降级路径（无 guard 时回退现有实现）、配置面（buzhou.tools.command.sandbox=disabled|process|guard-deno|guard-e2e）、审计与指标。产出 spec 17 + impl 60。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **port 放 core**（星形拓扑不动）：新 SPI `core/exec/CommandBackend`（`name()` + `run(shellCommand, workDir, timeoutSeconds)` 返回自含 `CommandOutcome` record——exit/stdout/stderr/timedOut/truncated，不引 guard 类型）。
2. **tools 消费**：RunCommandTool 增可选 `CommandBackend` 委托构造重载——有委托时黑名单/工作目录校验仍在 tools（前置语义不变），**执行**走 backend（沙箱自带隔离/白名单/超时），输出格式化含 truncated/stderr 段；无委托时现有 ProcessBuilder 路径不变（降级=不配置）。
3. **guard 桥接**：`SandboxCommandBackend implements CommandBackend` 包 CommandSandbox（`run` → `sandbox.run([/bin/sh,-c,cmd], PATH/HOME/LANG/TZ 白名单 env, workDir, timeout)`）；装配 `@ConditionalOnBean(CommandSandbox)` + `@ConditionalOnMissingBean(CommandBackend)`——应用注册什么沙箱档（Deno/E2B/Limited 组合）由应用定，guard 不替应用选档；程序式 `new SandboxCommandBackend(sandbox)` 直用。
4. **配置面简化**：`buzhou.tools.command.backend = builtin | sandbox`（默认 builtin，safe-by-default）。`sandbox` 时 tools auto-config 经 `ObjectProvider<CommandBackend>` 取实现——缺失启动 fail-fast（引 buzhou-guard + 注册 CommandSandbox bean 的修法指引）；沙箱运行时不可用（探测 false）不静默回退裸执行（IllegalStateException → 工具失败文本带 unavailableHint）。
5. **不做**：tools 侧再造 deno/e2b 档位枚举配置（档位选择归 guard 侧 bean 组合）；审计/指标最小化——结果文本显式 truncated/timeout 标记（沿用 tools 既有文本协议）。
