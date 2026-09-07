# Spec 104 — 模型失败签名接线（effort #66）

> wayfinder map：`.wayfinder/maps/effort-66.md`（T385–T386）。spec 83 fog 项收口。

## Problem Statement

ErrorSignatures（spec 83）只接了工具错误（kind=tool）：模型侧失败（上游 5xx 族、
超时族）不进 top 表——排障只见半边错误面。

## Solution

DefaultAgentSession.callModelWithinBudget 双路径旁路接线：
- 直调档（无 turnBudget）：catch RuntimeException → record("model", e) 后原样上抛；
- 守护档：TimeoutException → record("model", TIMEOUT)；ExecutionException →
  record("model", cause)。
既有抛出语义零变化（类型/消息/观测通知不变——签名是纯旁路观测）。

附带修正（#64 回归暴露）：ArchiveHealth 装配改 ObjectProvider<BuzhouStores>，
store 缺席（store.type 配错）报 UNKNOWN + disabled——不再抢启动期 store 校验的
报错优先级。

## User Stories

1. 作为运维，我要模型错误族与工具错误族同表，所以 top 排障不分家。
2. 作为宿主，我要失败签名零侵入，所以既有异常语义与测试零变化。

## Testing Decisions

- 异常族（IllegalStateException + 数字归一断言）；成功路径零记录；
  TIMEOUT 签名面；全量 core 回归（含启动校验双 fail 路径恢复绿）。

## Out of Scope

- stream onError 接线；签名导出。

## Further Notes

- 与 spec 83/85 组合：错误签名族（tool+model）→ 健康段 top-5 完整覆盖。
