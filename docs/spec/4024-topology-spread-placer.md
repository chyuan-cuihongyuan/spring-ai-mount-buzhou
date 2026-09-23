# Spec 4024 — 拓扑约束放置（effort #4024，R25）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6049–T6050，impl 2125）。
> 借鉴：K8s TopologySpreadConstraints maxSkew 语义。

## Problem Statement

「都堆热域」（机架/可用区/池间的容量失衡）——跨域散布的静态
硬约束裁决件缺失。

## Solution

`TopologySpreadPlacer`（core/policy，纯裁决）：

- 每域记现有数；放置看**放置后全域斜度**（max−min）≤ maxSkew
 ——拥挤域截止、宽松域可进；
- eligibleDomains 候选序（现有数升序、并列域名字典序——先填
  最空，确定性可回放）；
- skewOf 斜度读数（空图 0 诚实）；canPlace 单域裁决。

## User Stories

1. 作为放置作者，副本跨域散布有硬约束——热域自动截止。
2. 作为运维作者，先填最空的确定性序——放量过程可回放审计。

## Testing Decisions

- 均衡三域全可 + 斜度 0；偏斜恰界（放 b 后 5−3=2 可/放 a 后
  6−3=3 截止）；严档 maxSkew=1 交替填充演化；数升序确定性 +
  拥挤域截止并存；畸形六型 fail-fast + 空图诚实。

## Out of Scope

- 不做 whenUnsatisfiable 调度语义（DoNotSchedule/ ScheduleAnyway
  归调度器）；不做 minDomains 下限；不做 label 选择器。

## Further Notes

- 与 ConsistentHashRing 正交（容量面 vs 键空间面）。Wave 5
 （调度与放置族）开波。
- 里程碑：25/50。
