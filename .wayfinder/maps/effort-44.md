# Wayfinder Map — Buzhou 错误签名聚类（effort #44，50 轮自迭代第 9 轮）

> effort #44，延续 #43（T319–T320 / impl-229）。主线：工具/模型失败只有
> outcome=failed 计数——「哪一族错误在烧」不可见；Sentry fingerprint 的最小内核
> = 归一化签名 + 有界 top 表。

## Destination

`ErrorSignatures`（进程内有界注册表）：异常简名+归一化首行（数字串→#、长十六
进制→hex#、多行取首行、截 96）成族；256 条封顶折 `<kind>:__overflow__`（既有族
继续细分）；`top(n)`/`snapshot()`（count 降序+字典序稳定）；工具错误路径
（HookedToolCallback）接线；不进 micrometer tag（有界枚举纪律——签名维度天生
无界，故独立进程内表）。

## Notes

- 借鉴：Sentry fingerprint（错误聚合分组）；全局旋钮模式与 BuzhouMetricsHolder 一致。

## Decisions so far

- 进程内表而非 micrometer tag（tag 有界纪律；256 封顶由归一化+overflow 保证）。
- kind 前缀进签名（tool/model 分组）；overflow 按 kind 分开计数。

## Not yet specified

- model 调用失败路径接线（resilience 面已有错误码计数——接线点另议）；健康端点
  暴露 topErrors；签名导出（OLAP JSONL 面）。

## Out of scope

- 沿用 #7–#43；跨实例聚合（单实例语义，与 run registry 同口径）。

## Tickets

- [x] [T321 ErrorSignatures + 工具错误接线](../tickets/T321-error-signatures.md)（impl-230）
- [x] [T322 4 例红队（归一/封顶/top 稳定/throwable 面）+ 文档收口](../tickets/T322-signatures-close.md)
