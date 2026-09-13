# 686 — GateResult 有效通过率透出

**What to build:** GateResult 加 effectivePassRate 组件（11 参新构造 + 10 参兼容委托 NaN）+ enforce 填充 + 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] GateResult 组件 + 兼容构造
- [x] enforce 填充 + summary 附注
- [x] GateEffectiveRateTest（剪枝 run 填充精确/无剪枝相等/兼容构造/判定零变化）
- [x] spec 934 + README 行（欠账累计 926–934）

## Done

验证：`mvn -pl buzhou-core test -Dtest=GateEffectiveRateTest,EvalGateTest` 全绿。commit 见本轮 `feat(core)` 提交。
