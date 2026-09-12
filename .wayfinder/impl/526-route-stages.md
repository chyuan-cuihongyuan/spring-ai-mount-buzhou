# 526 — 路由金丝雀阶段标签

**What to build:** RouteStages（STABLE/CANARY/ARCHIVED 有界注册表）+ 纯函数 filter（构造期按 visible 集过滤候选权重，未标注保留，剔除计数）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] RouteStages（tag/stageOf/view + cap 64）
- [x] filter 纯函数（剔除 + 计数 + 未标注保留）
- [x] 三态/条件可见/零变化/cap/不可变用例
- [x] spec 723 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-resilience -am test` 绿。commit 见本轮 `feat(resilience)` 提交。
