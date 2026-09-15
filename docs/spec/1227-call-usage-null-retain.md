# 1227 — R28：adviseCall 路径 usage null 保留两侧补测

> 来源：K 会话第 28 轮 = effort #1226（[T1868](../../.wayfinder/tickets/T1868-call-usage-null-shape.md) / [T1869](../../.wayfinder/tickets/T1869-call-usage-null-verify.md) / impl 930）。方法论：R24 NullableUsage 先例在 call 路径的对称补齐。

## Problem Statement

recordModelCallOutcome 的 usage 属性跳过分支（getPromptTokens()==null → prompt 属性不写；getCompletionTokens()==null → completion 属性不写）在 call 路径从未被 null 值驱动——R16 用例经 DefaultUsage（null 归一 0）只覆盖了「0 值记录」侧。

## 目标

- completion-only（NullableUsage(null, 3)）：prompt 属性跳过、completion=3 照记。
- 双 null（NullableUsage(null, null)）：两属性均跳过。

## 实现决策

- NullableUsage record（getNativeUsage 桥）落 ObservabilityAdvisorCallTest（R24 流式先例对称）。

## 测试决策

- 断言只对外部行为：span 属性存在性与值。
- 验收门：定向绿 + observability 全量绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- recordTpotIfNeeded 计时分支（豁免不变）。

## Further Notes

- R24→R28：NullableUsage 先例从流式对称到 call 路径，null 保留语义全路径钉死。
