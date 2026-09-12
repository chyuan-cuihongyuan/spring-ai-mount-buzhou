# 533 — 路由阶段标签 yml 装配

**What to build:** buzhouWeightedChatModel bean 读 stages/visible-stages → RouteStages.filter 构造期过滤；<2 路 fail-fast；缺省零变化。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] bean env 读参 + filter 接线 + <2 fail-fast
- [x] 装配直调/缺省零回归用例
- [x] spec 730 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-resilience -am test` 绿。commit 见本轮 `feat(resilience)` 提交。
