# 1129 — 批级回喂预算（M 系 R29）

**What to build:** applyBatchBudget 贪心截大者 + Holder + autoconfig 键。

**Blocked by:** T2303 / T2304（同轮 shape+verify）。

**Status:** done

- [x] manager 预算字段 + setter + applyBatchBudget（降序贪心 + 标记 + 指标）
- [x] BatchResponseBudgetHolder + HarnessAssembler 拾取 + autoconfig
- [x] 三用例（截大留小/零预算透传/未超限不误伤）+ manager 12 用例零回归

## Done

验证：定向测试绿。commit 见本轮 feat 提交。
