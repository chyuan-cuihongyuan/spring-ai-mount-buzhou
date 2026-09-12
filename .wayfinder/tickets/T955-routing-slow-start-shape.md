---
id: T955
title: 模型端点慢启动权重爬坡的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

权重路由（spec 339/340）权重热调是瞬时跳变——热重载把某路权重从 1 提到 8 时瞬间吃到全量新配比，冷缓存/新建连的端点会被打爆。nginx upstream slow_start（恢复后权重渐进爬坡）怎么映射到本仓？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 3 轮 = effort #702 / spec 702 / impl 505）：新类 `RoutingSlowStart`（routing 包）——`ramp(route, target)` 把该路权重先落 floor（1）再按线性插值分 STEPS 步（常量 4）爬到 target；后台 daemon 定时器每 slowStart/STEPS 触发 `tick()` 推进（tick 同步方法、package 级可见——测试可直接手动推进零等待零抖动）；进行中 ramp 可升级 target（重算剩余步）；`ramps()` 不可变快照 + AutoCloseable。消费接线：`RoutingWeightsHotReload` 增可选 slowStart 构造参数——权重**上调**且 slowStart 开启时走 ramp（下调/持平仍瞬时：降权永远安全），slowStart=null（默认）行为逐字节不变。计数 `buzhou.routing.slow-start`（ramp 启动事件）。借鉴 nginx upstream `slow_start`（server 恢复后权重渐进）+ guava RateLimiter warmup 思想。
