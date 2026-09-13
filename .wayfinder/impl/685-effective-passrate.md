# 685 — 剪枝 run 有效通过率口径

**What to build:** EvalRunResult.prunedCount() + effectivePassRate() 派生方法 + 双口径测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] prunedCount() + effectivePassRate()
- [x] EffectivePassRateTest（稀释显形/全 pruned 约定/无剪枝双口径相等/既有零变化）
- [x] spec 933 + README 行（欠账累计 906–933 二十八行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=EffectivePassRateTest` 全绿。commit 见本轮 `feat(core)` 提交。
