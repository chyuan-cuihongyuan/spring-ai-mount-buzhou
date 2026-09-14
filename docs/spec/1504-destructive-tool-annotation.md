# 1504 — BuzhouTool destructive 风险注解

> 来源：M 会话第 5 轮 = effort #1504（impl 1107）。MCP tool annotations（destructiveHint）思想——工具自描述风险。

## 背景

`ToolsModule.enabledDangerousToolNames()`（供装配侧注册进 GuardModule 的 HITL 清单）为构造器内三处手工 `dangerous.add(...)` 硬编码：新增危险工具须改 ToolsModule 源码，漏登记则 HITL 守卫静默缺席。

## 目标

- `@BuzhouTool` 增加 `destructive() default false`（注解成员带默认值，源/二进制兼容）；
- 内置破坏性工具标注：write_file / run_command（含 SandboxRunCommandTool 沙箱版）/ http_request；
- `enabledDangerousToolNames()` 改为对已装配 tools 扫描注解生成（保装配序）；删除三处手工登记——行为等价（三内置工具名单不变），新工具标注即自动进名单。

## 兼容性

行为等价迁移；注解新增成员带默认值不破坏既有使用方；无注解的第三方工具不受影响。
