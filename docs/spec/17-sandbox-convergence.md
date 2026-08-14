# Spec 17 — run_command 与沙箱合流（mechanism）

> effort #5（T85 / impl-60）。此前 run_command（tools）与 guard CommandSandbox 双轨平行：
> 危险路径跑的是较弱的裸 ProcessBuilder + 黑名单。本 spec 合流且不动星形依赖图。

## 端口（core SPI）

- **`core/exec/CommandBackend`**（tools 与 guard 的唯一汇合点，均只依赖 core）：
  `String name()` + `CommandOutcome run(String shellCommand, Path workDir, long timeoutSeconds)`。
  `CommandOutcome` 自含 record：`exitCode / stdout / stderr / timedOut / truncated`——不引 guard 类型。

## tools 侧（消费方）

- **RunCommandTool 可选委托**：构造重载注入 `CommandBackend`。有委托时：
  黑名单与工作目录前置校验仍在 tools（拒绝语义不变），**执行**走 backend（沙箱自带
  隔离/环境白名单/超时/进程树击杀），输出格式化含 `stderr:` 段与 `truncated` 显式标记。
  无委托时现有 ProcessBuilder + env 白名单 + 杀进程树路径**不变**（降级 = 不配置）。
- **配置**：`buzhou.tools.command.backend = builtin | sandbox`（默认 `builtin`，safe-by-default）。
  `sandbox` 时装配经 `ObjectProvider<CommandBackend>` 取实现——缺失启动 **fail-fast**
  （修法指引：引入 buzhou-guard 并注册 `CommandSandbox` bean）。
- **不静默回退**：backend 运行时不可用（沙箱探测 false 抛 IllegalStateException）→
  工具失败文本携带 `unavailableHint`，绝不退回裸执行。

## guard 侧（提供方）

- **`SandboxCommandBackend implements CommandBackend`**：包 `CommandSandbox`——
  `run` → `sandbox.run([/bin/sh, -c, cmd], PATH/HOME/LANG/TZ 白名单 env, workDir, timeout)`，
  `CommandResult` 映射 `CommandOutcome`（killedReason=OUTPUT → truncated）。
- **装配**：`@ConditionalOnBean(CommandSandbox)` + `@ConditionalOnMissingBean(CommandBackend)`——
  应用注册什么沙箱档（Deno / E2B / Firecracker / Limited 组合）由应用定，guard 不替应用选档；
  程序式 `new SandboxCommandBackend(sandbox)` 直用。

## 不做

- tools 侧再造 deno/e2b 档位枚举配置（档位选择归 guard 侧 bean 组合）。
- Firecracker/E2E 完整实现（沿用 effort #2 out-of-scope 边界，接口预留不变）。
