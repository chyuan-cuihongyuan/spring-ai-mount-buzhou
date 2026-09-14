# 1126 — A/B 并行波间早停（M 系 R26）

**What to build:** PairwiseEvalRunner 并行路径分波化（spec 1522 扩散）。

**Blocked by:** T2297 / T2298（同轮 shape+verify；分波源头 T2295）。

**Status:** done

- [x] 分波执行 + 波间 earlyStop/hostCancel 检查
- [x] 16 用例零回归

## Done

验证：定向测试绿。commit 见本轮 refactor 提交。
