# 1210 — R11 分支批次 4：边缘分支清扫（ThinkingChainExtractor × DefaultSpanHandle）

> 来源：K 会话第 11 轮 = effort #1210（[T1829](../../.wayfinder/tickets/T1829-branch-uplift-batch4-shape.md) / [T1830](../../.wayfinder/tickets/T1830-branch-uplift-batch4-verify.md) / impl 913）。方法论：批次化延续——边缘分支（1–4 missed/方法）清扫与小类收口；ObservabilityAdvisor 流式 harness 单列后续轮（议程已入 map）。

## Problem Statement

批次 4 四类的残余分支：ThinkingChainExtractor（76%——ctor extraKeys 过滤链、stringOf 非 String、omitted 字符串形态）、DefaultSpanHandle（63%——attributes(Map) 批量导入从未被调用、attribute null 键、null error 防御、双 close 幂等、显式终态优先）。皆为防御分支与边界组合，属「小而行为敏感」档。

## 目标

- **ThinkingChainExtractorEdgeTest**（6 用例）：extraKeys null 列表 / blank·null·重复过滤、maxChars≤0 钳制为 1（truncated+originalLength）、blank 思维链值跳到下一 key、ATTR_OMITTED 字符串 "true" 形态、非 String 元数据值忽略。
- **DefaultSpanHandleBranchTest**（5 用例）：attributes(Map) 批量导入与 null 防御、attribute null 键防御、双 close 幂等、error 后显式终态保持 ERROR、null Throwable no-op。

## 实现决策

- DefaultSpanHandle 经 8 参公开构造直接构造 + RecordingBase extends BaseSpanRecorder（super(MicrometerDualWriter, false)，doEnqueue 录制）——RUNNING upsert 与终态 upsert 的入队计数可断言。
- AssistantMessage 元数据注入沿既有测试先例 `AssistantMessage.builder().properties()`（本版本 2 参元数据构造为 protected，不用反射）。
- BaseSpanRecorder.enqueue 为 RUNNING upsert + 终态 upsert 语义——用入队计数断言双 close 幂等（2 条而非 3 条）。

## 测试决策

- 断言只对可观察行为：构造参数过滤后的提取结果、入队条数、终态状态；不测内部 synchronized 块。
- 验收门：observability 全量绿 + 两类分支覆盖提升入账（ThinkingChainExtractor 76%→90%、DefaultSpanHandle 63%→88%）。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- ObservabilityAdvisor 流式 harness（R12 单列深做，议程已入 map）。
- ToolGraphAnalyzer（11 missed，图分析输入形状需读 230 行源码——批次 5 候选）。

## Further Notes

- observability 105 用例全绿；模块分支覆盖自 66.7%（R7 基线）显著上行。
