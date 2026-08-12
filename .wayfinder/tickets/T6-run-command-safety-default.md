---
id: T6
title: run_command 工具：默认关闭 vs 沙箱限制
type: grilling
status: open
assignee: ""
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

<!-- grilling 后填写：默认策略 + 沙箱方案（或否）+ 与 HITL 联动 -->
