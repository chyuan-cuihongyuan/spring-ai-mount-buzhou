# 722 — 工具泳道排队时延观测

> 来源：G 会话第 23 轮 = effort #722（借鉴 grpc server queue 时延观测——排队时间与执行时间分开计量）/ [T995](../../.wayfinder/tickets/T995-lane-wait-shape.md) / [T996](../../.wayfinder/tickets/T996-lane-wait-verify.md) / impl 525。

## 背景

LaneLimitingToolCallback（spec 205）的许可获取零排队观测——泳道满时调用者在等、等多久、多少超时被拒全不可见。Turn 慢时「泳道容量是否不足」无法归因；spec 108 的 tool duration timer 计的是含排队在内的总时长，排队分量不可分离。

## 目标

- `LaneLimitingToolCallback.withLane` 的 acquire 段 nanoTime 包裹：
  - 每实例 `WaitStats(waited, totalWaitNanos, maxWaitNanos, timeouts)` + `waitStats()` getter（实例面——装饰器由装配方持有，面板经装配可达）；
  - 指标 `buzhou.lane.wait`（timer；tag `lane=<名>`——泳道集配置有界，spec 111 合规；backend 共享许可路径无名不打 tag）；
  - 超时拒绌事件 `buzhou.lane.timeout`（tag 同上）+ timeouts 计数——异常语义不变。
- 计时纯观测——acquire/超时/release 语义零变化。

## 非目标

不做跨实例聚合（backend 共享泳道的分布式等待观测留位）；不做健康段（等待是正常态非失能）。

## 测试

阻塞计量、超时计量+语义不变、无竞争微耗时、既有零回归。

## 兼容性

additive 观测；行为零变化。
