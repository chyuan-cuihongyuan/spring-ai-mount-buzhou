# 654 — 评估失败率中途剪枝

**What to build:** EvalPrunePolicy record + EvalRunner.setPrunePolicy（串行路径观察窗+失败率剪枝，剩余项 status=pruned）+ 指标/WARN；默认关闭零变化；并行路径诚实不生效。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] EvalPrunePolicy record（minItems/failRateThreshold + 校验）
- [x] EvalRunItemResult.STATUS_PRUNED 常量
- [x] EvalRunner 串行剪枝循环 + pruned 结果生成 + 指标 + WARN
- [x] EvalPruneTest（恰停/观察窗/默认关/并行不受影响/阈值未达不剪）
- [x] spec 901 + README 行 + API 快照再生

## Done

验证：`mvn -pl buzhou-core test`（EvalPruneTest + 既有 EvalRunner 相关）全绿。commit 见本轮 `feat(core)` 提交。
