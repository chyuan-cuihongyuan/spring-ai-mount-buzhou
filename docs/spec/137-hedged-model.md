# Spec 137 — 模型对冲请求（effort #97）

> wayfinder map：`.wayfinder97/MAP.md`（T485–T486）。借鉴：Google tail-tolerant
> RPC / gRPC hedging policy——尾延迟用「并发押注」而非「更长等待」对冲。
> 与 spec 15 备模型降级链（终态失败后串行换人）互补：本能力管<b>长尾等待中</b>。

## Problem Statement

模型调用的 p99 长尾（偶发慢请求）会让整轮 Turn 等到超时才走降级链——用户体验
是「卡住很久才失败再重试」。长尾不是故障（主模型稍后也会回），串行降级对它
太钝；对冲请求（等超过 hedgeDelay 还没回就并发问备模型，先回先得）是标准解。

## Solution

`HedgedChatModel`（buzhou-resilience/fallback，`implements ChatModel` 装饰器）：

- **call(Prompt)**：主模型提交后等 hedgeDelay——已回则直接返回（零对冲成本）；
  未回即提交备模型<b>对冲</b>，先回先得，输家 future cancel(true)。
- **快速失败转对冲**：主模型在 hedgeDelay <b>内出错</b>（快速失败）不是终局——
  立即发备模型对冲等待（错误≠长尾，但也不必让用户吃一次确定失败的等待）。
- **双败**：两路都失败 → 抛<b>主模型</b>异常（保既有错误语义与降级链触发口径）。
- **stream(Prompt)**：诚实委派主模型（流竞速复杂度不成比例，显式不做）。
- 计数：`buzhou.hedge.primary-won` / `buzhou.hedge.fired` / `buzhou.hedge.won`。
- 诚实边界：同一 Prompt 原样发两模型（模型特定 options 兼容归宿主保证）；
  对冲成本 = 双倍调用（hedgeDelay 建议设在主模型 p95 之上）。

## User Stories

1. 作为宿主，偶发的慢模型调用不再拖垮整轮——超过阈值备模型接管，先回先得，
   用户感知从「卡 30s 失败」变「稍慢但成功」。
2. 作为运维，hedge.fired 高 = 主模型长尾劣化（该换/该查了），hedge.won 高 =
   对冲确实在救场——两个数字直接指导 hedgeDelay 调参。
3. 作为宿主，主模型健康时零对冲成本（先回不触发）。

## Implementation Decisions

- 装饰器实现 ChatModel（getOptions 委派主模型）；宿主把它当主模型挂进任何
  ChatClient/装配位——零侵入。
- 虚拟线程 executor 由调用方注入（生命周期归宿主）；对冲等待用 Future.get(delay)。
- 不改 ResilienceAdvisor / FallbackChain（集成面后续轮按需）。

## Testing Decisions

- 先回先得：慢主（sleep）+ 快备 → 备答案返回且总耗时 ≈ hedgeDelay+快备
  （远小于主模型耗时）。
- 主快不冲：主在延迟内返回 → 结果即主、fired 不增。
- 主快速失败转对冲：主抛错 + 备成功 → 备答案。
- 双败：主备都抛 → 主模型异常。
- 流委派：stream 走主模型（断言 seenPrompts 只主模型收到——备不被流打扰）。
- 桩：测试内手搓可控延迟 ChatModel（ScriptedChatModel 无延迟面）。

## Out of Scope

- 自适应 hedgeDelay（EMA）；stream 对冲；N 路对冲；对冲与熔断联动。

## Further Notes

- 韧性四件套：熔断（15）/降级链（15）/延迟感知排序（64）/对冲（本轮）。
