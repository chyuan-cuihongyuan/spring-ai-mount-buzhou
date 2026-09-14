# 1109 — A/B 对比 run 宿主取消（M 系 R7）

**What to build:** PairwiseEvalRunner.requestCancel() + PairwiseSummary.hostCancelled + 双路径项边界生效。

**Blocked by:** T2263 / T2264（同轮 shape+verify；取消语义源头 T2261）。

**Status:** done

- [x] requestCancel() + compare 开始清零 + 两路径未起项检查
- [x] PairwiseSummary 加 hostCancelled（9/7 参兼容构造器保留）+ 序列化/反序列化
- [x] 指标 buzhou.eval.ab.cancelled
- [x] PairwiseCancelTest 三面 + 既有 pairwise/SPRT 测试零回归

## Done

验证：定向测试绿。commit 见本轮 feat 提交。
