---
id: T6
title: run_command 工具：默认关闭 vs 沙箱限制
type: grilling
status: closed
assignee: zcode
blocked-by: []
created: 2026-08-13
---

## Question

`buzhou-tools` 的命令执行原子工具，安全默认应是——

- **默认关闭**（`buzhou.tools.run-command.enabled=false`，显式开启才注册）？
- 还是**默认开但沙箱**（命令白名单 / 容器 / 受限执行器）？
- 与机制⑧ Hook 护栏如何联动（危险命令模式强制 HITL 人工确认）？

## Context

- 用户明确项：run_command 默认关，或至少加沙箱。属 tools 层**安全收口**，虽非 core/memory/spill/guard，但「面向生产场景」必选。
- 权衡：默认关最安全但伤可用性；沙箱实现成本与跨平台（Linux/macOS/Windows）一致性是真问题。
- 偏 grilling（HITL）：安全模型是价值判断，需用户拍板。

## Resolution

**决策（implementer 确认既有实现的安全默认，可由用户推翻）**：**默认关闭**。

- **默认策略**：`buzhou.tools.run-command.enabled=false`（`ToolsModule.Builder` 既有默认），显式 opt-in 才注册 bean——开箱即用不暴露任何命令执行口子。
- **沙箱方案：否**。不引入额外沙箱执行器——① 跨平台沙箱（Linux/macOS/Windows 一致）实现成本高、与 alpha 定位不符；② run_command 已有多层防护（命令黑名单 `CommandBlacklist.defaults()` + workdir `FileSandbox` + 超时 + 输出兜底截断），opt-in 后再挂 HITL；③ 默认关已使「不暴露无限制命令执行口子」成立，沙箱是对「已 opt-in」场景的额外收紧、非必需。
- **HITL 联动**：opt-in 时 `ToolsModule.enabledDangerousToolNames()` 把 `run_command` 暴露给装配侧注册进 GuardModule 的 HITL 危险工具清单——tools 与 guard 安全模型统一、非各搞一套。
- **跨平台约束**：命令经 `/bin/sh -c` 执行，需 POSIX shell（Linux/macOS 原生；Windows 需 WSL/Git Bash）；已写入 `RunCommandTool` javadoc。默认关使此差异只影响显式开启者。

> **用户未应答 grilling**（本会话 AskUserQuestion 未获答复）→ 本 Resolution 为 implementer 按**既有代码 + 安全优先**原则的确认，记录在案；若用户偏好「默认开 + 沙箱」，可推翻重开本票。

**实现切片**：[impl/07](../impl/07-run-command-safe-default.md)。
