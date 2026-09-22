# Spec 1882 — 配额空间告警门（effort #1882，R83）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2965–T2966，impl 1483）。借鉴：
> etcd（48K+ 星）NOSPACE 告警语义——后端配额耗尽集群进入只读，拒绝
> 写但放行读；告警需显式解除（ack）而非自愈——「满即只读 + 人工
> 确认才能恢复写」的保守语义。

## Problem Statement

存储配额耗尽的处置两头险：自动恢复写会在清理完成前反复写满（抖动），
直接拒绝读写又把只读流量陪葬——缺一个「写拒读放 + 恢复须显式确认 +
水位回退账面」的确定性门。

## Solution

`QuotaAlarmGate`（core/policy，轻量持态门）：

- `onWrite(bytes)`：超配额即触发 NOSPACE——本次写拒绝、门保持
  triggered（写路径确定性拒绝）；
- `readsAllowed()`：读永不受告警影响；
- `usageRatio()`：用量/配额读数（触发前后都可感）；
- `acknowledge(minFreeBytes)`：显式解除——仅当释放后空闲 ≥ 阈值才
  复位（否则保持拒绝，防清理不足就恢复写的抖动）。

## User Stories

1. 作为状态存储运维者，配额 1GB 写满 → 写拒读放，清理到空闲 ≥
   100MB 后 ack 才恢复写——抖动免疫。
2. 作为评审者，ack 不足额即保持拒绝——「清了一点就想恢复」被门拦住。
3. 作为观测者，usageRatio 0.97 → 临近告警有预告。

## Implementation Decisions

- 持态小门（AtomicLong 用量 + volatile 告警态）；配额 ≥ 1、ack 阈值
  ≥ 0 fail-fast；读路径零锁。

## Testing Decisions

- 写满触发两态（拒绝/读放行）；ack 不足额保持拒绝、足额复位两例；
  usageRatio 精确；畸形三型 fail-fast。

## Out of Scope

- 不做实际存储清理（归 retention/fsck 面）；不做分布式告警广播
  （归共享 store 面）。

## Further Notes

- 与弹性预算池（#107）互补：那是软预算弹性伸缩，这是硬顶只读 +
  人工确认的保守门。
