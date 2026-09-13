# 661 — k 次 run 稳定性矩阵

**What to build:** EvalFlakinessDetector.analyzeK（跨 run 逐项对齐 + 红绿一致率）+ KStabilityReport/KItemVerdict record + 校验 + 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] analyzeK + KStabilityReport/KItemVerdict
- [x] EvalKStabilityTest（全一致/翻转/漂移/校验/两 run 版零回归）
- [x] spec 908 + README 行（欠账随下轮 README 解封一并补）

## Done

验证：`mvn -pl buzhou-core test -Dtest=EvalKStabilityTest` 全绿。commit 见本轮 `feat(core)` 提交。
