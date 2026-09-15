# Spec 1820 — 重启错峰计划（effort #1820，R21）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2841–T2842，impl 1421）。借鉴：
> memberlist/consul 协同重启错峰 + AWS Builders' Library jitter——同批实例
> 按稳定哈希分槽摊开重启，确定性无随机数。

## Problem Statement

配置热重载/故障恢复后，同批实例**同时**重试形成重启风暴（下游瞬时被打爆、
集体退避、循环共振）；随机抖动不可回放不可审计——错峰需要确定性且可预测
碰撞风险面。

## Solution

`RestartSpreadPlan`（buzhou-resilience，静态纯函数）：

- `delayFor(instanceId, cohortSize, spreadWindowMillis)`：slot =
  |id.hashCode| mod cohortSize，delay = slot × window / cohortSize ∈
  [0, window)——同 id 永远同槽（确定性可回放）；
- `cohort(cohortSize, window, ids)` → `CohortReport(instances, delays,
  maxDelay, collisions)`：槽碰撞累计（鸽笼诚实入账）+ collisionRatio
  （空批 -1 哨兵）。

## User Stories

1. 作为运维者，8 实例 1.6s 错峰窗 → 各实例拿到稳定互异延迟，重启摊开
   不打爆下游；重启计划可回放审计。
2. 作为容量治理者，collisionRatio 高 = 槽太挤，该加窗或缩批。
3. 作为框架宿主，零随机数零状态，纯排程可单测。

## Implementation Decisions

- 纯排程不执行；确定性（hash 槽）是核心设计约束（vs 随机 jitter 的
  不可回放）；碰撞不掩盖（诚实读数）。
- fail-fast：空白 id、cohortSize < 1、spreadWindow < 1；null 按空表。

## Testing Decisions

- 同 id 同延迟界内；8 实例 16 槽多样性；鸽笼碰撞账（5 实例 2 槽 ≥3 碰撞）；
  空批哨兵；畸形三型 fail-fast。

## Out of Scope

- 不执行重启；不做指数退避（那是单调用侧重试，归 JitterMode 族）。

## Further Notes

- 与 JitterMode 正交：那是单调用随机抖动，这是同批确定性错峰。
