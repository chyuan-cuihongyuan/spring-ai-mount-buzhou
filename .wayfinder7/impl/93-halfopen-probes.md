# impl-93 — 熔断半开多探测

**What to build:** half-open 连续 N 探测成功才恢复（默认 1 = 既有行为），抖动 provider 不再单探测循环。

**Blocked by:** None

**Status:** done

- [x] Circuit.halfOpenSuccessThreshold（8 参 canonical + 7/6 参兼容 + fail-fast）
- [x] 状态机：probesInFlight/halfOpenSuccesses；槽位不变量（在飞+已成功≥阈值拒绝）；失败立即回 OPEN（退避递增）
- [x] 测试：阈值 2 连续成功/中途失败回 OPEN（trips=2）/阈值 1 回归——resilience 84/84 绿
- [x] spec 35 §A

## Done

commit：见 git log（impl-93）。
