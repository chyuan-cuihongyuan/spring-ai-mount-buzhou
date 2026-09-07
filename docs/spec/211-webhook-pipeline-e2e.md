# Spec 211 — webhook 族组合 E2E（effort #225）

> wayfinder map：`.wayfinder/maps/effort-225.md`（T585–T586）。集成轮——五件叠成
> 一条链，一根测试钉住组合语义。

## Problem Statement

单件全绿 ≠ 组合可用：装饰顺序错（如 dedup 在 redactor 外——原文重复与脱敏
后重复指纹不同，去重失效）、事件形状在层间被改坏、fanout 下 fence 序错位
——这类集成缺陷单测全看不见。

## Solution

`WebhookPipelineE2E`（core/webhook 集成测试，零新生产代码）：

- **链**：`schema(declared) → dedup → redactor(PiiDetector+CustomPiiRules) →
  fanout(sink1 全量, sink2 错误族)`，sink 为 JDK HttpServer 收件。
- **断言五联**：坏事件（缺必备键）两 sink 均不达；重复事件只达一次；PII
  出站已脱（sink 收到占位符）；类型路由（sink2 只有错误族）；fence 连续
  CONTINUE（sink1 序号单调无 GAP）。

## User Stories

1. 作为宿主，五件按此链序装配即得全治理投递——组合被回归钉住不怕升级。
2. 作为贡献者，新投递件接入若破坏链语义，此测试先红——集成契约显式。

## Implementation Decisions

- 链序定案入档：schema 最外（坏事件最先拦、不占 dedup 指纹）→ dedup（重复
  不浪费脱敏）→ redactor（出站前最后脱）→ fanout。
- 复用各件既有测试桩模式（HttpServer + Collector）。

## Testing Decimals

- 五联断言即本 spec 的验收；另加链序可替换性 smoke（去 dedup 层仍可装配）。

## Out of Scope

- 性能基准；更多 sink；生产代码变更。

## Further Notes

- B 侧 webhook 族收官：lag（135）→ fanout（151）→ fence（159）→ redactor
  （177）→ dedup（203）→ schema（209）→ 组合 E2E（本轮）。
