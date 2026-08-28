# Spec 75 — A/B run 完成事件（effort #36）

> wayfinder map：`.wayfinder36/MAP.md`（T303–T304）。#35 fog 毕业生。
> 借鉴：LangSmith run 生命周期事件面。

## Problem Statement

EvalRunner 有 `eval.run.completed` 完成事件（spec 52 §F）；PairwiseEvalRunner 无对应
事件面：A/B 对比完成后，webhook 订阅方与看板不可感知，历史结论回溯（spec 74）与
实时感知不对齐。

## Solution

`compare()` 完成即发 `ab.run.completed`：独立收尾会话 `ab-<runId>-done`（A 路
runtime——基准面持有者）；payload 含 runId / datasetName / total / winsA / winsB /
ties / errors / winRateA / winRateB / durationMs；total > 0 才发（空集无对比发生，
与 eval.run.completed 同语义）；与落盘正交（2 参构造同样发——事件面只依赖执行本身）。

## User Stories

1. 作为看板作者，我要 A/B 完成即收事件，所以对比结果实时可见。
2. 作为 webhook 订阅方，我要 ab.run.completed 与 eval.run.completed 同口径，所以
   一套订阅代码覆盖两种 run。
3. 作为评估作者，我要空集 run 不发事件，所以事件语义保持「对比完成」不稀释。

## Implementation Decisions

- 收尾会话挂 A 路 runtime（B 路是被比较方，事件归属不对称无争议）。
- 事件与落盘正交：不落盘构造同样发（spec 74 的 2 参行为在落盘维度零变化，
  事件维度是家族对齐而非行为破坏）。

## Testing Decisions

- 完成事件 payload 与 summary 等值（winsA/winRate/durationMs 键存在）；
- 空集 run 零 eval./ab. 族事件；
- 2 参构造（不落盘）同样发事件且 store 无 ab.run. 键。

## Out of Scope

- 事件持久化/重放（outbox 已覆盖 at-least-once）；B 路事件归属配置化。

## Further Notes

- 事件类型族：`eval.run.completed` / `ab.run.completed`——前缀即 run 族名，
  订阅方按前缀过滤即可。
