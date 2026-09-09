# Wayfinder Map — Buzhou 非流式轮次错误回调对称化（effort #427，D 会话第 28 轮）

> D 会话第 28 轮（#423 扩散轮——R24 勘察旁注收口）。勘察：观察者契约
> {@code SessionObserver.onTurnError}「轮次异常终止时调用」——流式路径
> （failTurnOnce）回调，**非流式 chat() 的 RuntimeException 路径只
> recordTurnDuration 后上抛、不回调**。后果：ObservabilitySessionState
> 的 turn span 在非流式失败时悬空到会话关闭才收口且不带 error 状态
> （OTel span status=ERROR 语义缺失）；TurnErrorSampler（423）也采不
> 到非流式错误轮。

## Destination

DefaultAgentSession.doChatTurn 的 `catch (RuntimeException e)` 增
`observers.forEach(o -> o.onTurnError(turnSeq, e))`（与流式 failTurnOnce
同型）；LeaseLost 路径维持 abortTurnAsLeaseLost 专属语义（不双报）；
观察者异常不包裹（观察者契约轻量——与流式路径完全一致，不引入防御
性分歧）。

## Notes

- 号段：spec 427 / T745–T746 / impl-400。
- 借鉴源：OTel span status ERROR on exception（span 终态语义正确性）。
- 纪律：零新公共类型（快照 regenerate 零 diff 预判——R17 先例）；
  chatForEntity 经 doChatTurn 同路覆盖。

## Out of scope

- beforeTurn 钩子自身抛错的回调（模型调用缝是本轮覆盖面——诚实边界
  注记）；观察者异常隔离（与流式对称优先）。

## Tickets

- [x] [T745 非流式错误回调](../tickets/T745-nondrain-error-callback.md)
- [T746 观察者/span/采样三面用例](../tickets/T746-error-callback-semantics.md)
