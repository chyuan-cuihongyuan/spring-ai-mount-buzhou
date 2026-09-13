# 669 — pruned×稳定性×gate 联动补验

**What to build:** analyzeK/analyze 的 pruned 排除加固 + 联动 e2e（pruned 不产生假稳定 + gate×剪枝 run 正常）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] analyzeK pruned→null 排除路径
- [x] analyze 两 run 版同语义加固
- [x] PrunedStabilityE2ETest（假稳定探测/有效样本判定/gate×剪枝）
- [x] spec 916 + README 行（欠账累计 906–916 十一行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=PrunedStabilityE2ETest,EvalKStabilityTest` 全绿。commit 见本轮 `fix(core)` 提交。
