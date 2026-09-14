---
id: T2259
title: BuzhouTool destructive 风险注解（危险名单声明式化）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 5 轮：@BuzhouTool 增加破坏性风险维度并驱动危险工具名单？

## Resolution

**用户常设授权 AFK（可推翻）**

现状实证：ToolsModule.enabledDangerousToolNames()（供装配侧注册进 GuardModule HITL 清单）为构造器内三处手工 dangerous.add("write_file"/"run_command"/"http_request") 硬编码——新危险工具要么改 ToolsModule 源码要么漏登记（HITL 静默缺席）。

形状：① @BuzhouTool 加 destructive() default false（注解成员默认值，源/二进制兼容；MCP tool annotations destructiveHint 思想——工具自描述风险）；② write_file/run_command/SandboxRunCommandTool/http_request 标 destructive=true（read_file/todo 不标）；③ ToolsModule 尾部对已装配 tools 扫描注解生成名单（保装配序），删三处手工登记——行为等价迁移（三内置工具名单不变）+ 新工具标注即自动进 HITL 清单零源码改动。
