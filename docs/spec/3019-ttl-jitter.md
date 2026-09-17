# Spec 3019 — TTL 确定性抖动（effort #3019，R20）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5039–T5040，impl 2020）。
> 借鉴：缓存防雷群 jitter（memcached/AWS 实践）。

## Problem Statement

同批写入的键带同一 TTL 会同时到期——到期瞬间集体回源（cache
stampede/雷群）打爆源站；随机抖动又使各副本对同一键的过期时刻
不一致（一致性漂移）。

## Solution

`TtlJitter`（core/cache，纯函数静态件）：

- `jitteredTtlMillis(base, jitter, key)`：r∈[−1,1) 由键哈希
  （复用 DeterministicHash）归一派生，TTL = round(base×(1+r·jitter))；
- **按键确定性**：同一键跨实例跨重启恒同 TTL——副本一致 + 到期
  时刻天然错开两得（无需共享随机源，可复算可审计）；
- 带宽夹持 [base×(1−jitter), base×(1+jitter)]，下限兜底 1ms；
  jitter∈[0,1)、base ≥ 1、key 非空 fail-fast。

## User Stories

1. 作为缓存作者，同批键的到期时刻在带宽内铺开——雷群免疫。
2. 作为多副本作者，同一键各副本 TTL 恒等——无一致性漂移。

## Testing Decisions

- 同键跨调用恒等；1000 键带宽 [800,1200] 全夹持；双侧铺开
  （≥100 键早于/晚于 base——防全侧偏斜）；零抖动精确等于 base；
  base=1 强抖动下限兜底；100 键 ≥10 个不同 TTL（分散度）；四路
  参数 fail-fast。

## Out of Scope

- 不做概率早过期（stampede 预热留白——另一静脉）；不做分层 TTL；
  不接具体缓存（接线归调用方）。

## Further Notes

- 与 DecisionCache（TTL+LRU）互补：本件是 **TTL 生成口径**。
- 里程碑：20/150。
