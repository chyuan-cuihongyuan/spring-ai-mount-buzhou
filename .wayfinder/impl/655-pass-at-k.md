# 655 — pass@k 无偏估计器

**What to build:** EvalPassAtK 纯函数类（estimate 连乘无偏公式 + aggregate 逐项平均）+ 参数校验 + 论文已知值精确断言测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] EvalPassAtK.estimate（连乘形式）
- [x] EvalPassAtK.aggregate
- [x] EvalPassAtKTest（边界值/论文已知值/数值稳定/校验）
- [x] spec 902 + README 行 + API 快照再生

## Done

验证：`mvn -pl buzhou-core test -Dtest=EvalPassAtKTest` 全绿。commit 见本轮 `feat(core)` 提交。
