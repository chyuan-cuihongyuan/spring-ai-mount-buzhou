---
id: T2267
title: 危险工具默认 HITL 自动带入桥（S2 硬偏差修复）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2265
created: 2026-09-15
---

## Question

M 会话第 9 轮：design-incompleteness S2（危险工具 opt-in 启用时不自动带入默认守卫）如何修复？

## Resolution

**用户常设授权 AFK（可推翻）}

现状实证：spec 06/07 承诺「opt-in 启用时自动带入默认守卫条目」，ToolsModule.enabledDangerousToolNames() 模块外零消费方（BuzhouGuardAutoConfiguration 明言不自动耦合 tools）——Boot 用户显式打开 write_file 后无任何默认 HITL 拦截。

形状：core 提供进程级 DangerousToolRegistry 桥（双方只见 core，依赖白名单不破）——register/registered/reset 静态注册表（EvalRunRegistry 同款）；tools autoconfig 创建 toolsModule 后灌注 enabledDangerousToolNames；guard autoconfig 用 @AutoConfiguration(afterName="...BuzhouToolsAutoConfiguration") 字符串引用保装配时序（无需编译依赖），guardModule 构建时把注册表内每个名字按三参 dangerousTool 默认形态并入（yml 显式条目优先不重复）；开关 buzhou.guard.auto-dangerous-bridge（默认 true，显式 false 逃生）。MCP 动态清单桥接（连接后才知道工具名）记雾区候选。
