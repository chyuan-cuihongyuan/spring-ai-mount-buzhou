# 1125 — 并行路径失败率剪枝（M 系 R25）

**What to build:** EvalRunner 并行路径分波执行 + 波间失败率剪枝检查。

**Blocked by:** T2295 / T2296（同轮 shape+verify；剪枝源头 spec 901 / 取消 T2261 正交）。

**Status:** done

- [x] 分波提交（items 按 workers 分块）
- [x] 波间剪枝检查（串行同款观察窗语义）+ 剩余标 pruned
- [x] EvalParallelPruneTest + 既有 Prune/Cancel 零回归

## Done

验证：定向测试绿。commit 见本轮 feat 提交。
