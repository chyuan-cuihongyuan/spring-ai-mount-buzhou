# 528 — 路由慢启动 yml 装配

**What to build:** BuzhouRoutingProperties.slowStart 字段 + buzhouRoutingSlowStart 条件 bean + 热重载 ObjectProvider 接线；缺省零变化。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] properties 字段（null=关 / 负值 fail-fast）
- [x] 条件 bean + 热重载 ObjectProvider 接线
- [x] 三态绑定 + 接线直调用例
- [x] spec 725 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-resilience -am test` 绿。commit 见本轮 `feat(resilience)` 提交。
