# Spec 1906 — 分片偏斜审计（effort #1906，R107）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3013–T3014，impl 1507）。借鉴：
> Spark/ShardedDB（千 K 星级）data skew 语义—— hottest 分片负载 /
> 平均负载 = 偏斜比：1.0 绝对均衡、2.0 意味着热分片双倍承担——
> 重分片的触发线有刻度。

## Problem Statement

分片集群负载不均是常态：「总体吞吐还行」掩盖了热分片双倍承担、
冷分片闲置——max/avg 偏斜比没有独立判定面时，重分片时机全靠
故障后复盘。

## Solution

`ShardSkewAudit`（core/policy，静态纯函数）：

- `skewRatio(loads)`：max/avg——偏斜比（1.0 = 绝对均衡）；
- `needsReshard(loads, skewThreshold)`：偏斜比 ≥ 阈值 → 建议重
  分片；全零负载 fail-fast（无流量谈偏斜无意义）。

## User Stories

1. 作为分片运维者，{100,50,50} 偏斜 1.5、{90,10,10} 偏斜 2.45
   ——后者过阈值该动。
2. 作为容量评审者，阈值 2.0 为重分片触发线——主动式而非事后式。
3. 作为确定性评审者，纯函数同输入同输出。

## Implementation Decisions

- 纯函数零状态；loads 非空非负且 avg > 0 fail-fast；阈值 ≥ 1
  fail-fast。

## Testing Decisions

- 偏斜比两例（1.5/2.45 精确）；触发判定两侧行为；绝对均衡 1.0；
  畸形三型（空表/全零/负负载）fail-fast。

## Out of Scope

- 不做实际重分片执行（归调度层）；不做键空间再哈希。

## Further Notes

- 与 反热点重平衡（R15 descheduler 建议）互补：那是给出迁移动作，
  这是量化「该不该动」的判定；与 限流键热点读面互补：那是限流
  维度，这是负载维度。
