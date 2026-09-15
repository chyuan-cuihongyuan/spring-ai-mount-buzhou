# 1223 — R24：流式 usage 组合与 null 防御补测

> 来源：K 会话第 24 轮 = effort #1223（[T1861](../../.wayfinder/tickets/T1861-stream-nulls-shape.md) / [T1862](../../.wayfinder/tickets/T1862-stream-nulls-verify.md) / impl 926）。方法论：R19 行为语义选题的 null 组合面。

## Problem Statement

流式 usage 采集的 null 侧分支：completion-only（prompt 侧 null）与全 null Usage 的捕获口径，以及流中 null chatResponse 元素的 doOnEach 防御——各自行为从未被断言。

## 目标

- completion-only：prompt 侧 null → 不记 prompt_tokens、completion_tokens=3 照记。
- 全 null Usage：经 ChatResponseMetadata.builder() 归一为 0/0 捕获（实证 builder 归一行为——打点记录）。
- 流中 null chatResponse 元素：doOnEach 防御跳过、后续 chunk 照常处理。

## 实现决策

- NullableUsage record（prompt/completion 可 null + getNativeUsage 桥）——DefaultUsage 将 null 归一 0 无法驱动 null 分支（诊断打点实证）。
- 全部落 ObservabilityAdvisorStreamTest 追加（harness 复用）。

## 测试决策

- 断言只对外部行为：span 属性存在性与值。
- 验收门：定向绿 + observability 全量绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- firstMatch / ToolResponseMessage 已由 R19 覆盖。

## Further Notes

- 诊断插曲：TotalTokens 返回类型为 Integer（javap 实证）；DefaultUsage null→0 归一为 builder 链上游承担。
