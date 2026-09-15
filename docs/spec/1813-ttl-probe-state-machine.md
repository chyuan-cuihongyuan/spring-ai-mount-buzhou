# Spec 1813 — TTL 探针状态机（effort #1813，R14）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2827–T2828，impl 1414）。借鉴：
> Consul health check TTL——被探方在窗内心跳续命，过期靠时间自然到期判定
>（零轮询成本），非靠巡检发现。

## Problem Statement

健康判定多为二值（好/坏）且靠主动巡检发现坏——巡检有成本、二值无梯度：
「离翻脸还有多远」无处可问，续命在即的探针（该心跳没心跳）与新鲜探针
在二值里同形，错过提前处置窗。

## Solution

`TtlProbeStateMachine`（core/health，静态纯函数）：

- `evaluate(ageMillis, ttlMillis, warnFraction)` → 三态 `PASSING / STALE /
  CRITICAL`：到期线含上（age ≥ ttl 即 CRITICAL）；预警线含上（age ≥
  warnFraction × ttl 即 STALE）；
- `freshness(age, ttl)` 剩余新鲜度（1 − age/ttl，到期后钳 0 负值不外泄）；
- `census(ttl, warnFraction, samples)` 探针普查（三态计数 + criticalRatio，
  无探针 -1 哨兵）。

## User Stories

1. 作为健康治理者，STALE 探针集中出现 = 心跳续命在即的先兆——先查心跳
   链路再等 CRITICAL 打脸。
2. 作为值班者，freshness=0.2 的探针单上排前面——离翻脸 20% 距离，可运营
   梯度替代二值。
3. 作为框架宿主，TTL 与预警线口径自声明，纯判态零轮询零状态。

## Implementation Decisions

- 纯判态不执行（探活/摘除归宿主）；Consul 语义保留「到期靠时间不靠巡检」。
- fail-fast：负年龄 / TTL < 1 / warnFraction 越界或 NaN / 样本空 id；null
  按空表。

## Testing Decisions

- 三态 + 双边界含上；新鲜度线性与钳零；普查计数+占比+哨兵；畸形五型
  fail-fast。

## Out of Scope

- 不做心跳接收/存储（接线归后续轮）；不做自动摘除。

## Further Notes

- 与 BuzhouProbes（主动探针）互补：那是主动拨测，这是被动 TTL 到期。
