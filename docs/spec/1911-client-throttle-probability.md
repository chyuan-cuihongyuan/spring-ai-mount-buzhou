# Spec 1911 — 客户端自适应节流（effort #1911，R112）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3023–T3024，impl 1512）。借鉴：
> Google SRE 书自适应节流公式——客户端记住后端历史「请求数/接受数」，
> 拒发概率 = max(0, (req − K×acc)/(req+1))：后端过载时请求在客户端
> 就地衰减，比到了服务端再拒绝省一个来回。

## Problem Statement

后端过载时客户端照发不误（重试风暴放大器）：服务端限流虽保住了
后端，但网络与序列化成本已花——「按历史接受比在客户端概率性拒发」
缺独立计算面。

## Solution

`ClientThrottleProbability`（core/ratelimit，静态纯函数）：

- `rejectProbability(requests, accepts, ratioK)`：max(0, (requests −
  K×accepts)/(requests+1))——健康期（acc×K ≥ req）恒 0 不干扰；
  过载期概率随积压比爬升（分母 +1 防零）；
- `shouldDrop(probability, dice)`：dice ∈ [0,1) 掷骰——确定性可
  回放（dice < probability 即拒发）。

## User Stories

1. 作为客户端作者，历史 100 发 40 收、K=2 → 拒发率 ≈19.8%——
   过载期自动减速。
2. 作为健康期守护者，100 发 90 收 K=2 → (100−180) < 0 → 概率 0
   ——健康期零干扰。
3. 作为回放测试者，dice 注入确定性——同输入同输出。

## Implementation Decisions

- 纯函数零状态（历史计数归调用方）；requests/accepts ≥ 0、
  ratioK ≥ 1 fail-fast；分母 +1 防除零（首请求 1/1=… 有界）。

## Testing Decisions

- 健康零概率/过载正概率/极端积压近 0.5 三例；掷骰边界两例；畸形
  三型（负计数/负 K）fail-fast。

## Out of Scope

- 不做历史计数维护（归调用方统计）；不做服务端准入。

## Further Notes

- 与 RetryBudget（重试配额）互补：那是重试侧节流，这是首发侧
  节流；与 GradientAdaptiveLimiter 互补：那是并发上限，这是概率
  衰减。
