# Spec 209 — 事件 schema 检查器（effort #224）

> wayfinder map：`.wayfinder/maps/effort-224.md`（T583–T584）。JSON Schema required
> 面的最小可用版——事件 payload 语义层契约。

## Problem Statement

webhook 消费方按文档字段写代码，但发射侧无契约约束：字段改名/漏放静默上线，
坏事件流到下游才炸（消费方 KeyError）——传输层有信封契约（spec 20），语义层
（payload 必备什么键）裸奔。

## Solution

`EventSchemaChecker`（core/webhook）：

- **声明**：`Map<eventType, Set<必备键>>`（构造给定——事件族有界）。
- **检查**：`violations(event)` → 缺失键列表；未声明类型 = 无违规
  （open-world——新事件类型不被未更新声明卡死）。
- **装饰器**：包 SessionEventListener——违规事件默认<b>丢弃</b>（fail-closed
  保护下游）+ 计数 `buzhou.event.schema-violated`；显式 `failOpen=true` 时
  放行（调查期观察模式——违规可见但不拦）。

## User Stories

1. 作为消费方，收到的事件必有声明键——下游代码不再 KeyError。
2. 作为发射方（框架自身），声明即回归契约——漏放字段在出口被自己拦住。
3. 作为运维，violated 计数即「契约破坏率」——调查期 failOpen 观察不拦流。

## Implementation Decisions

- 最小面 required 键集（类型/枚举留档 JSON Schema 全量版）；空键集=该类型
  只验存在性。

## Testing Decimals

- 缺键拦且计数；全键过；未声明类型放行；failOpen 放行但计违规；空键集；
  组合装饰透传保真。

## Out of Scope

- JSON Schema 全量；声明 yml 化；自动生成。

## Further Notes

- 事件契约双层：信封（20，传输）+ payload 必备键（本轮，语义）。
