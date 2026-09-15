# Spec 1816 — 负载脱落阶梯（effort #1816，R17）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2833–T2834，impl 1417）。借鉴：
> Envoy overload manager / Akka 断路器组——过载处置按优先级阶梯逐级甩
>（低优先级先掉），而非一刀切全拒。

## Problem Statement

过载时的「一刀切全拒」让高价值请求与批量杂活同死——过载处置缺**优先级
语义**：哪些工作先掉、负载涨到哪一级掉什么，没有显式声明面，全凭运气
（谁后到谁死）。

## Solution

`LoadShedLadder`（core/backpressure，静态纯函数）：

- `Level(name, shedThreshold)` 阶梯级（契约：name 非空白、threshold ≥ 0
  非 NaN——低阈值即低优先级先掉）；
- `decide(loadFactor, ladder)` → `ShedDecision(loadFactor, shedLevels,
  keptLevels)`：负载因子 ≥ 级阈值即该级脱落（含边界）；
- `shedRatio()` 脱落面占比（无级 -1 哨兵）+ `escalating()` 升级信号。

## User Stories

1. 作为流量治理者，阶梯 [batch@0.5, interactive@0.8, critical@1.2]：负载
   0.6 只甩批量、1.5 才全甩——过载时「谁先死」显式声明。
2. 作为值班者，escalating() 从 false 翻 true 即过载早期信号；shedRatio
   爬升速度即恶化速度。
3. 作为框架宿主，负载因子口径（并发/利用率/队列深）自声明，纯裁决零状态。

## Implementation Decisions

- 纯裁决不执行（脱落动作归宿主）；与 PrefetchCreditWindow 互补：那是
  入口闸，这是过载后的分级甩。
- fail-fast：负/NaN 负载因子、空白名、负阈值；null 按空表。

## Testing Decisions

- 负载爬升逐级脱落三档；阈值边界含上；空表/null 哨兵；畸形四型
  fail-fast。

## Out of Scope

- 不执行脱落；不做动态阈值调整。

## Further Notes

- 与 EvictionThresholdGate（spill）正交：那是存储信号两级阈，这是流量
  负载多级阶梯。
