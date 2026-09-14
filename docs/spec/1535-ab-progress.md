# 1535 — A/B 对比进度读面（spec 1534 扩散）

> 来源：M 会话第 39 轮 = effort #1535（impl 1138）。

## 目标

`PairwiseEvalRunner.progress()` → `CompareProgress(runId, done, total, hostCancelled)`：过程快照（串行每项/波间）+ 聚合后终态快照（skipped null 占位无对象——终态统一 done=total）。

## 兼容性

纯新增读面零行为变化。
