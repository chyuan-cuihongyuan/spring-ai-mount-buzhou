# 1508 — 危险工具默认 HITL 自动带入桥（S2 硬偏差修复）

> 来源：M 会话第 9 轮 = effort #1508（impl 1111）。design-incompleteness S2 闭环：spec 06/07「opt-in 启用时自动带入默认守卫」承诺落地。

## 背景

`ToolsModule.enabledDangerousToolNames()` 模块外零消费方：Boot 用户显式打开 `write_file` 等危险工具后没有任何默认 HITL 拦截（与 safe-by-default 叙事相悖）。guard 与 tools 互不依赖（白名单硬性），装配期编排需要桥。

## 目标

- core 进程级 `DangerousToolRegistry`（register/registered/reset，EvalRunRegistry 同款静态注册表）；
- tools autoconfig 创建 `toolsModule` 后灌注 `enabledDangerousToolNames()`；
- guard autoconfig `afterName` 字符串引用保证装配时序，`guardModule` 构建时把注册表内名字按三参 `dangerousTool` 默认形态并入（yml 显式条目优先不重复）；
- 开关 `buzhou.guard.auto-dangerous-bridge`（默认 true；显式 false 逃生）。

## 兼容性

行为变化 = spec 承诺恢复：opt-in 危险工具默认有 HITL 拦截（此前为零拦截）；显式配置 yml 清单的用户条目优先；MCP 动态清单桥接为后续雾区（连接后才知道工具名，静态灌注不适配）。
