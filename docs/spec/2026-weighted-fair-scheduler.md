# Spec 2026 — 加权公平调度器（effort #2026，R27）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3153–T3154，impl 1577）。
> 借鉴：网络调度 DRR（Deficit Round Robin）——权重积分轮询公平分享。

## Problem Statement

多流共享出口（工具泳道 / 租户请求 / 事件消费）：严格优先级饿死低权
流，轮询无视权重——「高权重多得但不独占」的公平分享缺原语。

## Solution

`WeightedFairScheduler<T>`（core/exec，synchronized 小临界区）：

- `registerStream(id, weight ≥ 1)`（0 权永不服务 fail-fast 防配置
  错）；`enqueue`（未注册 fail-fast；空流转入活跃环且 **deficit 从零
  起步**——空闲不积累特权）；
- `pollNext()` **粘性轮内消费**（单项出口的 DRR 形态）：当前流积分
  可负担则继续出队（轮内不重入账——权重以突发形态兑现）；不足/空
  则让出游标，下一活跃流**入账 quantum×weight** 后消费；
- 长期公平：服务比 ≈ 权重比（heavy:light=3:1 长跑收敛）；空流退休
  清账出环；
- 读数：servedByStream（长期公平对账面）/ activeStreamCount。

## User Stories

1. 作为多租户出口作者，租户配额即流权重——3:1 权重长跑 3:1 服务，
   低权流不断流。
2. 作为对账者，servedByStream 直读公平性——偏离权重比即有积压偏流。

## Testing Decisions

- 3:1 权重 300 项长跑比 ∈ [2.7,3.3]；单流独占排干；等权严格交替；
  quantum=10×w5=50 突发窗口 heavy 领先（light 可能零服务——getOrDefault
  口径）；空闲流无积累特权（清账再入从零）；空返 null；畸形七型
  fail-fast。

## Out of Scope

- 不做抢占（轮询非强占）；不接泳道/租户装配（接线归后续轮）。

## Further Notes

- 首版教训入档：单项出口 DRR 若无粘性轮内消费，deficit 累积但每轮
  只兑现 1 项——权重失效（长期比 1:1）；粘性让权重以突发兑现。
