# Spec 43 — 命令限额与配置校验（effort #9）

> effort #9 主线 spec。§A：run_command 输出兜底上限可配（T157）；§B：配置校验补全（T158）。

## §A run_command 输出内存兜底上限可配（T157 / impl-128）

### Problem Statement

图前勘察误判纠偏（诚实记录）：进程树强杀（超时/取消两路 killProcessTree）与输出内存兜底
（readBounded 5MB 截断）**已存在**（impl-49/60）。真实缺口只剩：5MB 兜底上限硬编码不可配——
低内存部署（如 512MB 容器）无法收紧，进程输出洪峰仍可先吃满兜底再截断。

### Solution

输出内存兜底上限可配（编程构造参 + `buzhou.tools.run-command.max-output-bytes`），缺省 5MB
不变；截断语义钉住：截断标记可见、进程照常跑完、exit 码照常上报。上下文治理仍归 Spill
offload（两层互不替代——本上限是 OOM 防护不是上下文防护）。

### User Stories

1. As a 运维工程师, I want 按部署规格收紧输出兜底上限, so that 低内存容器不被进程输出洪峰冲击。
2. As a 应用开发者, I want 截断发生时看到明确标记, so that 输出缺失可发现不静默。
3. As a 应用开发者, I want 非正上限启动即拒, so that 配错兜底不如不配的问题立刻暴露。

### Implementation Decisions

- `RunCommandTool` 增七参构造（maxOutputBytes；正数 fail-fast）；`DEFAULT_MAX_OUTPUT_BYTES`
  公开常量 5MB；readBounded/truncate 实例化按配置上限截断，标记文本携带实际上限值。
- `ToolsModule.Builder` 增 `runCommandMaxOutputBytes`（yml 键 `run-command.max-output-bytes`；
  非正数 BuzhouConfigurationException）；缺省 null → 工具默认。
- 沙箱委托版（SandboxRunCommandTool）输出治理归 backend 域，不重复实现（诚实边界）。

### Testing Decisions

- tools 单测（POSIX 门控，Prior art RunCommandToolTest）：10KB 上限 + 100KB 产出 → 标记可见 +
  体积贴近上限；缺省 5MB 行为钉住（6MB 产出截断）；非法上限构造拒绝。

### Out of Scope

- rlimit/cgroup 资源限额（纯 JDK 不可移植；沙箱档 SandboxLimits 已有，内置档黑名单+环境白名单
  +输出上限+进程树强杀即内置档的完整防线）。

## §B 配置校验补全（占位，T158 落地时补全）
