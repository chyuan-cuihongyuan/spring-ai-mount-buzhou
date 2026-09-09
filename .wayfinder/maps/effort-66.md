# Wayfinder Map — Buzhou 模型失败签名接线（effort #66，50 轮自迭代第 31 轮）

> effort #66，延续 #65（T383–T384 / impl-250）。主线：**spec 83 fog 项「model
> 失败路径签名接线」**——ErrorSignatures 只接了工具错误（kind=tool），模型侧
> （超时族/上游 5xx 族）缺席，排障 top 表只有半边。

## Destination

DefaultAgentSession.callModelWithinBudget 双路径接线：直调档（无 turnBudget——
catch RuntimeException 记签名后原样上抛）；守护档（TimeoutException → TIMEOUT
签名；ExecutionException → cause 签名）。既有抛出语义零变化（异常类型/消息/观测
通知全不变）。附带修正 #64：ArchiveHealth 改 ObjectProvider 装配 + store 缺席
UNKNOWN-disabled（不抢启动期 store 校验的报错优先级——BuzhouStartupValidationTest
回归红队暴露）。

## Notes

- 借鉴：spec 83 家族补齐（Sentry「全错误面聚类」语义）。

## Decisions so far

- 签名记录不改抛出路径（纯旁路观测——lenient，不因签名失败影响调用方）。

## Not yet specified

- stream 路径 onError 签名（doOnError 面另议）。

## Out of scope

- 沿用 #7–#65。

## Tickets

- [x] [T385 模型双路径签名接线 + ArchiveHealth 装配修正](../tickets/T387-model-signatures.md)（impl-251）
- [x] [T386 3 例红队（异常族/成功零记录/超时面）+ 全量 core 回归 + 收口](../tickets/T388-model-signatures-close.md)
