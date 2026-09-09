# Spec 122 — superstep 原子批（effort #86）

> wayfinder map：`.wayfinder/maps/effort-86.md`（T443–T444）。#85 收口 fog 种子①
> 「事务性并行批（LangGraph superstep）」的最小可落地版。

## Problem Statement

并行工具批按 per-tool 语义各自校验各自执行：批内 a 合法、b 入参违规时，a 可能已经
产生副作用（写文件/发请求），随后 b 的校验反馈才把整轮打回 REASK——宿主视角
「半个批执行了、半个没执行」，且已执行的副作用无人叫停。LangGraph 把一个并行批视为
一个 superstep（原子步）：批要幺整体推进、要么整体不发生。

## Solution

`HarnessToolCallingManager` 增 opt-in 开关 `atomicBatchValidation`（默认 **false** =
既有 per-tool 行为零变化）：

- **前检（atomicPreflight）**：批派发前对所有 toolCall 依次做「工具存在 + 入参 schema
  校验」（纯内存、零锁零许可）；任一未过 → **整批不派发**：
  - 违规者回喂校验反馈（既有 REASK 通道，结局 `VALIDATION_REJECTED`，计入
    validationFailures 供停止条件裁决）；
  - 合法同伴回喂「同伴参数校验未过，本批原子中止（未执行）」（结局
    `BATCH_ABORTED`——`ToolCallOutcome` 新维度，事件日志可查「未执行因同伴」）。
- 全过 → 正常并行派发（与既有路径完全一致）。
- 工具缺失同样计入前检门（整批中止，缺失者回喂 missing-tool 反馈）。
- 诚实边界：本能力管「执行前派发」；已派发执行中的失败仍走既有 FAILED_ONLY/ALL
  回喂策略（副作用不回滚，不谎称事务回滚）。

## User Stories

1. 作为宿主，我开启原子批后，批内任何参数违规都不再伴随「合法同伴已执行了一半」的
   脏副作用，模型一次 REASK 即可重试整个批。
2. 作为运维，我能在事件日志里用 BATCH_ABORTED 结局区分「未执行因同伴违规」与
   「执行了但失败」。
3. 作为宿主，我不开启该开关时，一切行为与升级前一致（零行为变化承诺）。

## Implementation Decisions

- 开关落 `HarnessToolCallingManager`（经 `SessionAssemblyContext.toolManager()` 注入，
  与 batchFeedbackPolicy / argsValidation 同通道）。
- `ToolCallOutcome` 追加 `BATCH_ABORTED`（枚举追加，既有序列化不受影响）。
- 前检逐项零锁：不占组锁/并发许可/派发槽——中止批的等待成本为零。
- 中止批的同伴反馈走 `ToolErrorFeedback.format` 词汇通道（EXECUTION_FAILURE 标记 +
  明示「未执行」），模型可读且机器可分类。

## Testing Decisions

- 只测外部行为（回喂文案与执行与否），不测内部方法；先例
  `HarnessToolCallingManagerTest`。
- 四象限：开-违规中止整批（合法同伴 callback 零调用）/ 开-全过正常执行 /
  开-工具缺失中止 / 默认关 per-tool 行为不变（回归钉）。
- 事件日志断言：BATCH_ABORTED 条目按 toolCallId 落盘。

## Out of Scope

- 执行期失败的回滚（副作用回滚不可实现，不做也不谎称）。
- yml 配置绑定与 starter 装配（后续轮按需）。

## Further Notes

- 与 spec 13 `BatchFeedbackPolicy.FAILED_ONLY` 正交：一个管派发前、一个管执行后。
