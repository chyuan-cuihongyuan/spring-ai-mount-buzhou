# Spec 1896 — 事务号余量分级（effort #1896，R97）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2993–T2994，impl 1497）。借鉴：
> PostgreSQL（15K+ 星）xid wraparound 防线——事务号 32 位循环，
> 消耗逼近上限即分级告警（WARN/CRITICAL），耗尽即停写自保
> （数据库拒绝新事务强制清理）。

## Problem Statement

单调递增的资源编号（事务号/序号/代数）耗尽前无分级预警：临期
不清理、临期不降载，到耗尽那一刻以「拒绝一切写入」的方式暴雷
——余量分级（OK/WARN/CRITICAL/EXHAUSTED）缺独立判定面。

## Solution

`XidHeadroomGuard`（core/recovery，静态纯函数 + Urgency 枚举）：

- `urgency(consumed, limit, warnAt, criticalAt)`：分级判定——
  consumed ≥ limit → EXHAUSTED；≥ criticalAt → CRITICAL；
  ≥ warnAt → WARN；否则 OK（边界含上）；
- `headroom(consumed, limit)`：剩余量读数（负 = 已超发，诚实显示）。

## User Stories

1. 作为存储运维者，limit 1000、warnAt 800、criticalAt 950：消耗
   960 → CRITICAL——清理窗口还剩但必须动手。
2. 作为容量作者，临界线语义「含上」——恰在 950 即 CRITICAL 不含糊。
3. 作为兜底者，EXHAUSTED 即停写自保——拒绝新事务是最后防线。

## Implementation Decisions

- 纯函数零状态；limit ≥ 1、0 ≤ warnAt ≤ criticalAt ≤ limit、
  consumed ≥ 0 fail-fast；判定只读不写。

## Testing Decisions

- 四级各一例（OK/WARN/CRITICAL/EXHAUSTED）；边界含上两例
  （恰 warnAt/恰 limit）；余量读数；畸形三型（分级线倒置/线越
  limit/消耗为负）fail-fast。

## Out of Scope

- 不做真实事务号分配（归存储层）；不做清理执行（归 retention）。

## Further Notes

- 与 QuotaAlarmGate（etcd 空间只读）互补：那是字节配额告警门，
  这是单调编号余量分级；与保留策略（#65）互补：那是清理策略，
  这是清理紧迫度。
