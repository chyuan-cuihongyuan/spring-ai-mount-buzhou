# 1604 · 响应缓存 stale-if-error（Varnish grace / RFC 5861 思想）

> 来源：N 会话 R5（effort #1604 / T2359–T2360 / impl 1157）。借鉴对象：Varnish
> grace mode 与 HTTP RFC 5861 `stale-if-error`（Cache-Control 扩展）——后端故障时
> 继续服务「刚过期」的缓存内容，可用性优先于新鲜度。

## Problem Statement

响应缓存（spec 53）TTL 过期即弃。供应商故障窗口（熔断全开 / 网络分区）内，同一请求
反复穿透到已知的失败路径——明明几秒前还有可用答案（只是刚过期），用户面对的却是硬错误。
HTTP 缓存世界对此有成熟答案：stale-if-error 允许在过期后的一段时间内、且仅在上游
失败时，用旧内容救场。

## Solution

`buzhou.resilience.response-cache.stale-window`（Duration，默认 0=关）：

- store：过期条目在宽限窗内**保留不弃**（正常 get 仍 miss——新鲜度语义不变）；
  新增 `getStale(key)`——条目已过期且仍在窗内 → 返回旧响应（staleReads 计数）；
  超窗照弃（救场机会有时限）。
- advisor：`adviseCall` 的模型调用抛 RuntimeException 时先 `getStale` 救场——
  命中则返回旧响应（异常不抛），无救场条目则**异常照抛**（失败语义不静默吞——
  救场是显式策略，不是错误过滤器）。
- 流式（adviseStream）不救场（out-of-scope：流式聚合半途失败的救场语义复杂，
  独立裁决）。

## User Stories

1. 作为用户，我想在模型故障期拿到几秒前的相同答案，所以短暂的供应商故障不至于变成硬错误。
2. 作为运维者，我想让救场可观测，所以 staleReads 计数显示故障期被救回的请求数。
3. 作为运维者，我想保持默认零变化，所以不配 stale-window 时过期即弃照旧。
4. 作为开发者，我想失败不被静默吞，所以无救场条目时异常原样抛出。

## Implementation Decisions

- `ResponseCacheStore` 主构造器扩参 staleWindow（既有构造器委托 ZERO——零行为）；
  get 过期路径分「宽限内保留 / 超窗弃」两态。
- `ResponseCacheAdvisor.adviseCall` try/catch 救场（coalescing 路径的 leader 失败
  等待者各自直调语义不变——救场只挂直调路径）。
- 配置 `ResponseCache` record 扩参 + 5 参兼容构造保留。

## Testing Decisions

- `ResponseCacheStaleIfErrorTest`（MutableClock + FailingChain 伪链）：
  ① 宽限内过期条目保留、get miss、getStale 命中 + 计数；超窗 empty 且条目被弃；
  ② 默认 0=关：过期即弃、getStale 恒 empty（零变化）；
  ③ advisor 失败救场：FailingChain 抛异常 + 宽限内过期条目 → 返回旧答案不抛；
  ④ 无救场条目 → 异常照抛（消息原样）。
- Prior art：`ResponseCacheWeightBudgetTest`（MutableClock）、`IdempotencyAdvisorTest`（伪链）。

## Out of Scope

- 流式救场（半截流 + 缓存重组的语义需独立裁决）。
- 语义缓存的 stale-if-error（相似度语义下「过期」边界更模糊，若有需求另立 spec）。

## Further Notes

- 救场响应不添加注记前缀（不污染对话内容）；可观测走 staleReads + 故障期日志关联。
