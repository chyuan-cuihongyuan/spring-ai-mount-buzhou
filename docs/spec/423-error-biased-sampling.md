# Spec 423 — 错误偏向采样（effort #423）

> wayfinder map：`.wayfinder/maps/effort-423.md`（T737–T738）。D 会话第 24 轮。

## Problem Statement

TurnSamplerHook（spec 407）均匀采样且只挂 afterTurn——错误轮不走
afterTurn（failTurnOnce 只调 onTurnError）：失败轮在采样面是盲区；
低基础率下最有评测价值的错误输入几乎全被淘汰。OTel tail_sampling
的「ERROR 全保、成功按基础率」规则缺失。

## Solution

`eval.TurnErrorSampler`（SessionObserver 缝——onTurnStart 记输入、
onTurnError 采错、onTurnEnd 清场）：

- `error-rate-percent` 默认 100（错误轮全保——tail_sampling ERROR 规则）；
  确定性 hash 采样（与 407 同公式 floorMod(sessionId:turn,100)<rate——
  同轮同判可复现）。
- 入集 response 占位 `[TURN-ERROR] 异常类: 消息`（消息 ≤200 截断）——
  采样语义=进候选池，golden 仍是人工判断（407 同口径）。
- yml：`buzhou.eval.error-sampling.{enabled, dataset, error-rate-percent,
  min-input-chars}`；装配经 assemblyCustomizer `ctx.addObserver`（per
  session，sessionId 取自装配 ctx）。
- EvalDatasetStore bean 条件放宽：任一采样 enabled 即暴露（单 store
  不双 bean——error-only 宿主不必开基础采样）。

## User Stories

1. 作为评测集维护者，我想错误轮全量或高率进候选池，so 罕见失败输入
   不因低基础率丢失。
2. 作为宿主作者，我想只开错误采样不开基础采样也能用，so 小流量灰度
   有渐进路径。

## Implementation Decisions

- fail-soft：入集异常吞 + `buzhou.eval.error-sampling-failed` 计数
  （旁路绝不炸轮）；成功计数 `buzhou.eval.error-sampled`。
- 线程语义：reactor 信号串行化保证 onTurnStart/onTurnError 相互
  happens-before——普通字段持有当前输入即可（注记）。
- 观察者 per-session 实例（customizer 每会话 new）——无共享态。

## Testing Decisions

- 20 个不同 turn 的错误轮 rate=100 全入集（偏向性对照：基础采样同
  键率 5 远做不到）；rate=0 零入集；空输入/短输入过滤；onTurnEnd 后
  的迟到 error 不采（清场语义）；response 占位含 [TURN-ERROR] 与
  异常类名、长消息截断。
- yml：双开全装配；error-only 出 store+error RC、无成功 hook。
- E2E：ScriptedChatModel.enqueueThrow → chat 抛错 → 集内 1 条。

## Out of Scope

- 错误分类学分桶 rate；双率合一 hook；修复环中段错误。

## Further Notes

- 新公共类型 `TurnErrorSampler`、`BuzhouErrorSamplingProperties` 随轮
  regenerate 快照。
