# 702 — 模型端点慢启动权重爬坡

> 来源：G 会话第 3 轮 = effort #702（借鉴 nginx upstream `slow_start` / guava RateLimiter warmup）/ [T955](../../.wayfinder/tickets/T955-routing-slow-start-shape.md) / [T956](../../.wayfinder/tickets/T956-routing-slow-start-verify.md) / impl 505。

## 背景

加权路由热调权（spec 340）是瞬时跳变：把恢复/新加入路的权重从 1 提到 8，下一 tick 就吃到全量新配比——冷缓存、未建连的端点被瞬时打爆，往往又触发离群驱逐（spec 601）来回震荡。nginx 的洞察：server 上线/恢复后权重从 0 渐进爬坡（slow_start），给连接池与缓存暖机时间。

## 目标

- `RoutingSlowStart`（routing 包）：`ramp(route, target)`——该路权重立即落 floor（1），按线性插值分 STEPS=4 步爬到 target；后台 daemon 定时器每 slowStart/STEPS 触发 `tick()` 推进（tick 包内可见——测试手动推进，零等待零抖动）。
- 进行中 ramp 可升级 target（重算剩余步、仍收敛）；重复 ramp 未完成同路由 = 升级而非并行双 ramp。
- `ramps()` 不可变快照（route → {target, current}）；AutoCloseable 关停调度器。
- 接线：`RoutingWeightsHotReload` 可选 slowStart 参数——权重**上调**且开启时走 ramp；下调/持平仍瞬时（降权永远安全）；`null`（默认）行为逐字节不变。
- 计数：`buzhou.routing.slow-start`（ramp 启动事件，BuzhouMetricsHolder 全局面）。

## 非目标

不做 yml 装配面（装配轮顺延——spec 702 装配计划在本会话第 35 轮）；不改 WeightedRouter 本体（爬坡语义在 routing 层，router 仍是纯 WRR 原语）。

## 测试

routes() 观测 + 手动 tick()：floor 起步 → 逐步递增 → 终点恰 target → ramps() 清空；ramp 升级 target 收敛；热重载上调触发 ramp / 下调瞬时；slowStart=null 零回归；close() 终止。

## 兼容性

opt-in 纯增量；默认构造路径逐字节不变。
