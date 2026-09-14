# 1108 — EvalRunner 评估 run 协作式取消（M 系 R6）

**What to build:** requestCancel() 实例级协作取消 + STATUS_CANCELLED 状态 + 串行/并行双路径项边界生效。

**Blocked by:** T2261 / T2262（同轮 shape+verify）。

**Status:** done

- [x] volatile 标记 + requestCancel() + run 开始清零
- [x] 串行/并行两路径剩余项 STATUS_CANCELLED（与 pruned 分立）
- [x] 指标 buzhou.eval.run.cancelled
- [x] EvalRunnerCancelTest（触发/未触发/残留清零三面）+ 既有 Prune/Budget 零回归

## Done

验证：定向测试绿。commit 见本轮 feat 提交。
