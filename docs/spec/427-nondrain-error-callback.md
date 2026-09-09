# Spec 427 — 非流式轮次错误回调对称化（effort #427）

> wayfinder map：`.wayfinder/maps/effort-427.md`（T745–T746）。D 会话第 28 轮。

## Problem Statement

`SessionObserver.onTurnError`「轮次异常终止时调用」只在流式路径
（failTurnOnce）兑现——非流式 chat() 的 RuntimeException 路径只
recordTurnDuration 后上抛、不回调。后果：turn span 悬空到会话关闭
且无 error 状态（OTel span status=ERROR 缺失）；TurnErrorSampler
（423）采不到非流式错误轮。

## Solution

`DefaultAgentSession.doChatTurn` 的 `catch (RuntimeException e)` 增
`observers.forEach(o -> o.onTurnError(turnSeq, e))`——与流式
failTurnOnce 完全同型：

- LeaseLost 路径维持 abortTurnAsLeaseLost 专属语义（不双报）。
- 观察者异常不包裹：观察者契约轻量，与流式路径对称优先（不引入
  防御性分歧）。
- chatForEntity 经 doChatTurn 同路覆盖。

## User Stories

1. 作为可观测性用户，我想非流式失败的轮次 span 立即带 error 收口，
   so 失败可见性不依赖会话关闭。
2. 作为评测维护者（423 扩散），我想非流式错误轮也进候选池， so
   采样面不因调用形态而盲。

## Implementation Decisions

- 零新公共类型（行为修复轮——快照 regenerate 零 diff 预判，R17 先例）。
- beforeTurn 钩子自身抛错不在覆盖面（模型调用缝是本轮边界——诚实
  注记）。

## Testing Decisions

- 记录观察者：enqueueThrow 非流式 chat → onTurnStart 后 onTurnError
  收到原异常、onTurnEnd 不发生；同会话后续成功轮照常（onTurnEnd）。
- TurnErrorSampler 非流式 E2E（R24 用例改回 chat 路径）：错误轮入集。

## Out of Scope

- 钩子抛错回调；观察者异常隔离。

## Further Notes

- 无新公共类型——快照零 diff 预判。
