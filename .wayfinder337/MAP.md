# Wayfinder Map — Buzhou 工具上下文行李（effort #337，C 会话第 38 轮）

> C 会话第 38 轮。多租户/多环境宿主里，工具常需要知道「这次调用属于谁/
> 哪个环境」（tenant/env/region/关联 id）——现状只能拼进提示词（模型
> 可见可篡改、token 有价）或每工具自建全局。W3C Baggage / OTel baggage
> 的解法：带外键值上下文随调用传播——不进提示词、直达工具。

## Destination

`ToolBaggage`（core.exec：per-runtime 有界可变键值面——yml 静态播种 +
运行时 put/remove API；64 键/256 字符值封顶）+ HarnessToolCallingManager
传播点（executeToolCalls 的 ToolContext 注入 `buzhou.baggage` 快照——
空行李零注入零开销）+ HarnessAssembler.withToolBaggage + 装配
（buzhou.tools.baggage.<k>=<v> 播种；bean 恒在——325 事故按钮同「必须
预先在场」纪律，宿主运行时注入不依赖 yml）。

## Notes

- 号段：spec 337 / T665–T666 / impl-360。
- 借鉴源：W3C Baggage / OpenTelemetry baggage（带外上下文传播）。
- 纪律：行李只进 ToolContext 绝不进提示词/模型可见面；快照注入（工具
  读到调用时刻的一致视图）。

## Decisions so far

- per-runtime 作用域（宿主一 runtime 一套行李——多租户宿主多 runtime
  隔离）；session 级动态行李留待需求出现（诚实边界）。
- 注入的是不可变快照而非活引用——工具内不会看到行李中途变化。

## Out of scope

- session 级行李 API；行李加密/脱敏（值约定为非敏感路由元数据）；
- 跨进程传播（HTTP 头序列化——MCP 出网关另议）。

## Tickets

- [x] [T665 ToolBaggage 有界键值面](tickets/T665-tool-baggage.md)
- [x] [T666 传播点 + 装配 + 收口](tickets/T666-baggage-wiring.md)
