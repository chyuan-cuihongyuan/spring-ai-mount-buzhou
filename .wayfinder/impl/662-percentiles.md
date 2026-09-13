# 662 — 评估分数分位数读面

**What to build:** EvalScoreAnalytics.percentiles（R-7 线性插值）+ 校验 + 已知值测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] percentiles（R-7 插值 + LinkedHashMap 保序）
- [x] EvalPercentilesTest（已知值/单调/单样本/校验/保序）
- [x] spec 909 + README 行（欠账：906–909 四行，README 竞争解除后一并补）

## Done

验证：`mvn -pl buzhou-core test -Dtest=EvalPercentilesTest` 全绿。commit 见本轮 `feat(core)` 提交。
