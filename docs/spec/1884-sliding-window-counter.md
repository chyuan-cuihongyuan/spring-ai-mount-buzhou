# Spec 1884 — 滑动窗口计数器（effort #1884，R85）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2969–T2970，impl 1485）。借鉴：
> Cloudflare（万星级技术博客）sliding window counter——两个固定窗
> 计数按时间流逝比插值：无须逐请求记账（滑窗日志的 O(n) 内存），
> 拿到近似的滑窗速率。

## Problem Statement

限流器的窗口两头难：固定窗在边界处可突发 2×（两窗相接盲区），滑窗
日志要逐请求记账内存大——固定窗计数 + 插值修正的「滑窗计数器」
语义没有独立计算面。

## Solution

`SlidingWindowCounter`（core/ratelimit，静态纯函数）：

- `estimate(prevWindow, currWindow, elapsedRatio)`：prev×(1−ratio) +
  curr——流逝比插值的滑动速率估计（ratio=0 窗初→≈prev 惯性延续，
  ratio=1 窗末→curr）；
- `wouldExceed(prevWindow, currWindow, elapsedRatio, limit)`：估计值
  ≥ 限值即超——下一请求准入判定。

## User Stories

1. 作为限流作者，prev=100 curr=0 窗初估计 ≈100——上窗速率惯性
   防边界突发。
2. 作为容量评审者，prev=100 curr=100 ratio=0.5 → 150——两窗
   插值比固定窗 100 的账更接近真实滑窗。
3. 作为确定性测试者，纯函数零状态同输入同输出。

## Implementation Decisions

- 纯计算零状态；计数 ≥ 0、elapsedRatio ∈ [0,1] fail-fast；边界
  「= 限值即超」按满额拒绝语义。

## Testing Decisions

- 插值四例（窗初惯性 100/窗末 curr/中点 50/双百 150）；准入判定
  两例（=limit 拒、<limit 放）；畸形三型（负计数/ratio 越界）
  fail-fast。

## Out of Scope

- 不做逐请求记账与原子扣减（归 RateLimitBackend 面）；不做多实例
  共享（归 Redis 后端）。

## Further Notes

- 与令牌桶/GCRA（令牌/合同语义）互补：那是状态机限流，这是固定
  窗基础设施上的零状态估计修正。
