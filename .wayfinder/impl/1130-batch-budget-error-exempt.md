# 1130 — 批预算错误反馈豁免（M 系 R30）

**What to build:** applyBatchBudget 跳过 isErrorFeedback 候选。

**Blocked by:** T2305 / T2306（同轮 shape+verify；预算源头 T2303）。

**Status:** done

- [x] 豁免逻辑 + 四用例（新增错误反馈豁免例）

## Done

验证：定向测试绿。commit 见本轮 fix 提交。
