# 473 — 熔断时间窗衰减

**What to build:** Circuit.time-window（默认 0）+ ModelCircuit 平行时间戳 ring 与时间过滤率计算。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 配置组件 + 校验 + 兼容构造
- [x] 时间过滤率计算与 min-cals 门（Clock 注入确定性）
- [x] 3 用例绿 + resilience 240/240 零回归
- [x] spec 620 + README 行

## Done

验证：`mvn -pl buzhou-resilience -am test` 绿。commit 见本轮 `feat(resilience)` 提交。
