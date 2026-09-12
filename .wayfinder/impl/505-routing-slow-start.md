# 505 — 模型端点慢启动权重爬坡

**What to build:** RoutingSlowStart（floor 起步线性 STEPS 步爬坡 + daemon tick + ramps() 快照 + close）+ RoutingWeightsHotReload 可选接线（上调走 ramp、下调瞬时、null 零变化）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] RoutingSlowStart（ramp/tick/ramps/close，STEPS=4 常量 + floor=1）
- [x] 热重载可选 slowStart（上调 ramp / 下调瞬时 / null 零变化）
- [x] buzhou.routing.slow-start 计数
- [x] 手动 tick 五组用例 + 既有热重载零回归
- [x] spec 702 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-resilience -am test` 绿。commit 见本轮 `feat(resilience)` 提交。
