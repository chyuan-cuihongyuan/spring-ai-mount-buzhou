# 1218 — R19：Advisor 流式语义定向补测（快照提取 × TTFT 幂等 × omitted-only × 空信号）

> 来源：K 会话第 19 轮 = effort #1218（[T1845](../../.wayfinder/tickets/T1845-stream-detail-shape.md) / [T1846](../../.wayfinder/tickets/T1846-stream-detail-verify.md) / impl 921）。方法论：行为语义选题（不经行号）——「这条流式语义若回归，生产会怎样坏」反推用例。

## Problem Statement

流式 harness（R13）落地后仍有四条行为语义无直接用例：① captureInjectionSnapshot 的 ToolResponseMessage 占位符提取（微压缩 evidence/spill 回注格式的快照还原承诺）② TTFT 首信号 CAS 幂等（多内容块只打点一次）③ omitted-only 流块（Anthropic OMITTED 元数据流式形态）④ 空 Flux（仅 complete 信号）的 doOnEach null 防御。

## 目标

- 流式（ObservabilityAdvisorStreamTest 追加 4 用例）：TRM chunk 的 THINKING 事件与 evidence/spill 快照提取；双内容块 STREAM_FIRST_TOKEN 恰一次；omitted-only 无 THINKING 无内容信号；Flux.empty 正常关闭 span。
- 非流式（ObservabilityAdvisorCallTest 追加 1 用例）：adviseCall 快照含 evidence/spill 的 SnapshotMessage 字段断言。

## 实现决策

- ToolResponseMessage 构造 protected → 测试内静态子类桥接（super(List.of(ToolResponse), Map.of())）；正文含 [evidence:ev-9] 与 [spill:s-1/2] 模式文本做提取断言，无模式变体断言 null。

## 测试决策

- 断言只对外部行为：事件类型序列、快照 SnapshotMessage 字段、STREAM_FIRST_TOKEN 恰一次。
- 验收门：定向绿 + observability 全量绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- recordTpotIfNeeded 的 perTokenNs≤0 计时分支（不硬凑）。

## Further Notes

- evidence/spill 提取为 memory/spill 模块回注格式的快照还原承诺（spec 03 推演 #15）首次直接断言。
