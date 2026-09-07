# Spec 337 — 工具上下文行李（effort #337）

> wayfinder map：`.wayfinder/maps/effort-337.md`（T665–T666）。C 会话第 38 轮。

## Problem Statement

多租户/多环境部署中，工具需要知道调用的归属上下文（tenant、env、
region、关联单号）：现状要么拼进提示词（模型可见可篡改、每次调用
重复付费），要么每个工具自建全局配置（散乱且无法按 runtime 区分）。
带外上下文传播缺失。

## Solution

W3C Baggage / OTel baggage 思想——带外键值上下文随调用传播：

- **`ToolBaggage`**（core.exec）：per-runtime 有界可变键值面——
  `put/remove/clear/isEmpty` 运行时 API + `view()` 不可变快照；
  64 键封顶、键非空、值 256 字符封顶（防御面）。
- **传播点**：HarnessToolCallingManager 在 executeToolCalls 构造
  ToolContext 时注入 `buzhou.baggage` = 行李快照（空行李零注入零开销；
  注入快照非活引用——工具读到调用时刻一致视图）。行李只进 ToolContext，
  绝不进提示词/模型可见面。
- **装配**：`buzhou.tools.baggage.<k>=<v>` 静态播种；ToolBaggage bean
  恒在（325 事故按钮同纪律——运行时 API 必须预先在场，不依赖 yml）；
  HarnessAssembler.withToolBaggage 链式接入。

## User Stories

1. 作为多租户宿主，我想按 runtime 配置 tenant/env 等路由元数据，所以
   工具按归属隔离资源而无需模型转述。
2. 作为工具作者，我想从 ToolContext 读 `buzhou.baggage` 快照，所以
   上下文可信（模型不可见不可篡改）。
3. 作为运维，我想运行时动态增删行李不重启，所以 关联单号/灰度标记
   等临时上下文即挂即用。
4. 作为使用者，我想没配行李时工具调用零额外开销，所以 不用白不用
   付费的心智负担。
5. 作为审计者，我想行李有界（键数/值长封顶），所以 恶意或失控的
   行李不会撑爆 ToolContext。

## Implementation Decisions

- per-runtime 作用域：宿主一 runtime 一套行李（多租户宿主多 runtime
  天然隔离）；session 级动态行李留待需求（诚实边界记档）。
- 注入不可变快照；key 常量 `buzhou.baggage`。
- HarnessToolCallingManager 经 setter 接收（构造器链已长——不再加参）。

## Testing Decisions

- ToolBaggage：put/view 不可变/空键拒绝/键数与值长封顶/isEmpty。
- 传播（DeadlinePropagationTest 同手法）：有行李工具读到快照/空行李
  无键/构造后 put 即下次调用可见（活面快照语义）。
- 装配：yml 播种 bean；无 yml bean 恒在空视图。

## Out of Scope

- session 级行李 API；跨进程/跨服务传播（HTTP 头——MCP 网关另议）；
  行李加密脱敏（值约定为非敏感路由元数据——敏感内容走 333/336 加密面）。

## Further Notes

- 新公共类型 `ToolBaggage` 随轮 regenerate 快照。
