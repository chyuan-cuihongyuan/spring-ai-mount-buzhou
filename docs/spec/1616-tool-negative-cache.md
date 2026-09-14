# 1616 · 工具失败负缓存（DNS negative caching 思想）

> 来源：N 会话 R17（effort #1616 / T2383–T2384 / impl 1169）。借鉴对象：DNS
> negative caching（NXDOMAIN 短 TTL 缓存，RFC 2308）——否定答案也值得缓存，
> 但 TTL 必须短。

## Problem Statement

模型反复调用同一失败工具是真实负载：参数校验失败后原样重试、下游故障期重试
风暴——每次都穿透到工具执行层。成功缓存（spec 183 TTL memo）明确不缓存失败
（可重试信号），但「短窗内的同一失败」重复支付执行成本没有意义。

## Solution

`NegativeCachingToolCallback`（装饰器族，宿主 wrap）：
- key = 工具名 + argsHash（spec 183 同口径）；失败判定 = 结构化错误标记
  （[工具执行失败]/[工具参数校验失败]）或抛出的 RuntimeException。
- 失败 → (errorText, expireAt=now+negTtl) 入 LRU（256 封顶）；TTL 内同 key
  复读直接回缓存错误文本（negativeHits 计数）——不再真调。
- **TTL 到期即恢复窗口**（DNS 语义：窗内故障即使已恢复也不放行——短 TTL 纪律
  兜底，默认 30s）；成功不入负缓存（成功归 spec 183 族）。
- 设计修正记录：初版含「成功即清除」机制，测试暴露其不可达（TTL 内短路返回
  错误文本，真调永远不发生）——删除，恢复窗口 = TTL 本身，语义诚实。

## User Stories

1. 作为运维者，我想让重试风暴不再穿透工具层，所以同 key 失败窗内复读零执行成本。
2. 作为开发者，我想让故障恢复不被长窗卡住，所以负 TTL 默认 30s 且可配。

## Testing Decisions

- `NegativeCachingToolCallbackTest` 四断言：失败缓存窗内拦截（三次复读真调 1 次）；
  TTL 过期放行真调；异常路径同缓存（二次同参不抛、返回缓存文本）；不同参数独立 key。
- 文案语义钉住：结构化标记前缀（isErrorFeedback 按前缀判定——裸文案不入缓存）。

## Out of Scope

- 负缓存命中的 early-probe（命中率衰减/概率放行探测——若 30s 窗过粗再立项）。
- yml 装配面（装饰器族 169 先例——宿主 wrap）。
