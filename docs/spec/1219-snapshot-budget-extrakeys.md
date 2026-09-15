# 1219 — R20：注入快照预算分解断言 + thinkingExtraKeys 贯通 + 文本 null 防御

> 来源：K 会话第 20 轮 = effort #1219（[T1847](../../.wayfinder/tickets/T1847-snapshot-budget-shape.md) / [T1848](../../.wayfinder/tickets/T1848-snapshot-budget-verify.md) / impl 922）。方法论：行为语义选题——「从未被断言的产出字段」即测试缺口（预算分解字段自 impl-46 落地后零断言）。

## Problem Statement

captureInjectionSnapshot 产出的 InjectionSnapshot.budgetBreakdown（tokens.system/user/assistant/total 四键 + messages.count）自落地起零断言——角色分桶预算是「注入视图成本可解释」的承诺面。同时 ThinkingChainExtractor 的 extraKeys 厂商扩展仅有 extractor 级单测，经 advisor 的端到端贯通（config→extractor→THINKING 事件 provider.key）无用例。

## 目标

- 快照预算分解（Prompt 注入 SYSTEM+USER+ASSISTANT 三角色）：budgetBreakdown 四键确定性断言（tokenEstimator 长度口径）+ messages.count + SnapshotMessage role/messageId 形状（SYSTEM:0 等）。
- thinkingExtraKeys 贯通：advisor 构造注入含 custom_thinking 的 extractor → 助手元数据 custom_thinking → THINKING 事件 provider.key=custom_thinking + thinking.available=YES。
- 文本 null 防御：AssistantMessage content=null → 无 FINAL_REPLY、无 NPE。

## 实现决策

- 全部落 ObservabilityAdvisorCallTest 追加 3 用例（harness 复用）；PendingSnapshot instanceof 分流取快照记录。
- tokenEstimator 沿测试桩（长度口径）→ 预算断言确定性。

## 测试决策

- 断言只对外部行为：快照记录字段、事件 provider.key、异常不外泄。
- 验收门：定向绿 + observability 全量绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- 装配层 config→extractor 的贯通（autoconfiguration 级，他线域）。
- recordTpotIfNeeded 计时分支（不硬凑）。

## Further Notes

- budgetBreakdown 首次断言 = 「从未被断言的产出字段即测试缺口」方法论的延续实例。
