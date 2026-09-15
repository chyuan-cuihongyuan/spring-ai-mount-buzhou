# 1225 — R26：Advisor 细粒度残余清扫（文本 null 防御 × TRM 占位符正反例）

> 来源：K 会话第 26 轮 = effort #1225（[T1865](../../.wayfinder/tickets/T1865-advisor-fine-shape.md) / [T1866](../../.wayfinder/tickets/T1866-advisor-fine-verify.md) / impl 928）。方法论：细粒度残余的可达性分级——可达面清扫、不可达面（计时依赖/上游契约保证）如实豁免。

## Problem Statement

ObservabilityAdvisor 残余 ~70 missed 中多数为防御/计时分支，但仍有两条可达语义无用例：流式 chunk 的 AssistantMessage 文本 null（getText null → reply 跳过）与 captureInjectionSnapshot 的 ToolResponseMessage 占位符提取正反例（evidence/spill 模式匹配 vs 无模式 null）。

## 目标

- 流式：content=null chunk → 无 FINAL_REPLY、无 NPE、后续照常。
- call 侧：TRM 含 [evidence:ev-9]/[spill:s-1/2] → SnapshotMessage 字段填充；无模式文本 → null（firstMatch null 分支）。

## 实现决策

- TRM 经静态子类桥接（R19 先例）；全部落既有两测试类追加。

## 测试决策

- 断言只对外部行为：事件缺席、字段 null/值、异常不外泄。
- 验收门：定向绿 + observability 全量绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- recordTpotIfNeeded 计时分支（不可达）。
- markFirstToken subscribedAt==0 分支（doOnSubscribe 前置不可达）。

## Further Notes

- 可达性分级方法：防御分支三类——上游契约不可达（豁免）、计时依赖（豁免+注记）、真实可达（补测）。
