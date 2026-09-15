# 1226 — R27：T1867 TRM getText 聚合语义核验（观察票闭环）

> 来源：K 会话第 27 轮 = effort #1226（[T1867](../../.wayfinder/tickets/T1867-trm-placeholder.md) / impl 929）。方法论：观察票核验——javap 反编译 + 生产路径源码比对 + 删除按错误合同写的断言。

## Problem Statement

T1867 观察票记录：R26 快照占位符正反例补测中，TRM 占位符提取的实际行为与两种候选合同（builder 聚合 textContent / getResponses responseData）无法调和——直接断言 `trm.getText()` 含模式恒假红，快照提取却实测成功。

## 目标

- javap 反编译 + HEAD 源码比对定论：getText() 恒空（设计现状），生产捕获走 getResponses() responseData。
- 删除按错误合同写的断言；保留正反例提取断言（快照 SnapshotMessage evidence/spill 字段）。
- T1867 闭票（观察闭环：疑点不成立）。

## 实现决策

- 断言修正：删除 `getText() contains 模式` 断言（恒 "" 假红根因 = 按错误合同断言）；快照提取合同由 snapshotPlaceholderExtractionPositiveAndNegative 的正反例承载。

## 测试决策

- 验收门：ObservabilityAdvisorCallTest 定向绿 + observability 全量绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- HookChain.run 分发语义的文档化（R22 已入档）。

## Further Notes

- 证据链：javap 反编译（protected 构造 ldc "" 传 AbstractMessage）+ HEAD captureInjectionSnapshot 的 getResponses 遍历实现——双源一致。
