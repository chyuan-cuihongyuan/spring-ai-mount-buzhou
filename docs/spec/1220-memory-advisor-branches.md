# 1220 — R21：BuzhouMemoryAdvisor 分支直测（写路径 fence × 去重 × 角色过滤）

> 来源：K 会话第 21 轮 = effort #1220（[T1849](../../.wayfinder/tickets/T1849-memory-advisor-shape.md) / [T1850](../../.wayfinder/tickets/T1850-memory-advisor-verify.md) / impl 923）。方法论：批次化延续——「行为敏感优先」选题（写路径 fence 是双主窗口零写入承诺的执行面）。

## Problem Statement

BuzhouMemoryAdvisor（BRANCH 10 missed / 77%）承载记忆写入三承诺：fence 先于落库（双主窗口零写入）、Identity 实例去重（同消息不重写）、USER/ToolResponse 写入与 ASSISTANT 过滤。此前仅装配级间接触达，分支从未逐一执行。

## 目标

- BuzhouMemoryAdvisorTest（8 用例）：conversationId 缺失直通；USER+ToolResponseMessage 写入与 prompt 重建（rebuilt=memory.get）；instructions 内 ASSISTANT 过滤；同实例去重不重写；fence 先于落库计数、抛错阻断零写入、恢复后照常；after 落 assistant（fence 一次）；null chatResponse 与 conversationId 缺失 no-op；无 fence（null=无租约语义路径）行为不变。

## 实现决策

- BuzhouChatMemory + InMemoryMessageStore 真件复用（无 Mockito）；ToolResponseMessage 经 builder（构造 protected）。
- fence 以计数 Runnable 注入 + 可注入抛错，恢复阶段清 throw 后重建 advisor。

## 测试决策

- 断言只对外部行为：memory.get 内容、rebuilt prompt 指令、fence 计数、异常类型；不测 seen 集合内部态。
- 验收门：定向绿 + BuzhouMemoryAdvisor 分支 95% 入账 + buzhou-core 定向绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- HookAdvisor（6 missed，beforeModel/onModelError 切面分发）留 R22。

## Further Notes

- 同实例去重用 IdentityHashMap 语义——断言「同实例不重写」而非 equals 相等（不同实例同内容会重复写——设计如此，轮次内去重以实例为准）。
