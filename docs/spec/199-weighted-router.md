# Spec 199 — 平滑加权路由（effort #218）

> wayfinder map：`.wayfinder218/MAP.md`（T571–T572）。借鉴：Nginx smooth
> weighted round-robin——按权重分流且分布平滑（5:1:1 不出现连五爆发）。

## Problem Statement

等价模型多供应商（互为热备又分担流量）需要按权重分流：简单轮转不能表达
「A 供 5 份 B/C 各 1 份」；纯随机分布毛刺且不可复现。Nginx 的平滑加权轮转
是业界标准解：比例精确、时间上平滑、同序可复现。

## Solution

`WeightedRouter<T>`（core/concurrent，泛型）：

- **构造**：`(candidate, weight>0)` 有序列表；空表 pick = empty。
- **算法**：每候选 current += weight；取最大者选中，其 current -= totalWeight
  （Nginx 同款——比例精确且平滑）。
- **动态调权**：`setWeight(candidate, w)` 即时生效零重建；`weights()` 观测。
- 单候选恒选；线程安全（单锁——pick 是轻临界区）。

## User Stories

1. 作为宿主，三供应商 5:1:1 分流——比例精确、请求错开（不集中打一家）。
2. 作为运维，某供应商劣化 setWeight 降权——流量即时回撤，零重建零重启。
3. 作为测试，同序 pick 序列确定——路由行为可断言可回归。

## Implementation Decisions

- 确定性序列（非随机）——平滑算法天然可复现。
- 泛型候选（模型/工具实例/任何可命名对象——等价identity by equals）。

## Testing Decimals

- 7 次 pick 5:1:1 分布精确且无连五爆发（首 7 序列断言）；单候选恒选；
  动态调权后比例即时变化；零权重拒绝；空表 empty；并发 pick 不炸（smoke）。

## Out of Scope

- 随机策略；一致性哈希；最少连接。

## Further Notes

- 分流组合面：健康过滤（149/195）→ 加权分流（本轮）→ 降级兜底（15）。
