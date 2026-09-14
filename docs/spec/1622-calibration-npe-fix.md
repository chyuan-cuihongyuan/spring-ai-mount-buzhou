# 1622 · R19 对账 NPE 修复（跨会话记档承接）

> 来源：N 会话 R23（effort #1622 / T2395–T2396 / impl 1175）。

## Problem Statement

spec 1618（R19）的校准对账接线在 `TokenBudgetHook.afterModel` 直接解引用
`ctx.request().prompt().getInstructions()`——测试替身链路（只带 response 的
ModelCallContext）NPE，CounterAtomicitySpreadTest 挂（M 会话 spec1513 隔离
验证时发现并记档归属本会话）。

## Solution

对账前置三重缺席防御：`request()` / `prompt()` / `getInstructions()` 任一
null 跳过对账（纯观测旁路缺席不记即诚实——对账本就不影响预算语义）。

## Testing Decisions

CounterAtomicitySpreadTest 恢复绿 + CalibrationAuditHolderTest /
TokenBudgetHookEndToEndTest 零回归。

## Further Notes

- 流程教训：观测旁路接线必须对 ctx 可选字段做缺席防御（替身链路是合法形态）。
- 跨会话记档承接：并行会话发现的非己线缺陷按归属修复——自迭代流程自愈闭环。
