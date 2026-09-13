# 656 — bootstrap 均值置信区间

**What to build:** EvalScoreAnalytics.bootstrapMeanInterval（Efron percentile，seed 注入可复现）+ MeanInterval record + 校验 + 统计性质测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] bootstrapMeanInterval + MeanInterval record
- [x] EvalBootstrapCiTest（同 seed 可复现/区间有序含点估计/覆盖性近似/校验）
- [x] spec 903 + README 行

## Done

验证：`mvn -pl buzhou-core test -Dtest=EvalBootstrapCiTest` 全绿。commit 见本轮 `feat(core)` 提交。
