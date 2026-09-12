# Wayfinder Map — Buzhou 评估 run 预算闸（effort #520，E 会话第 21 轮）

> E 会话第 21 轮（budget 族扩散到 eval run）。勘察：eval run 逐项真调
> 模型（judge 也烧 token）——run 无预算上限：坏 judge/死循环数据集 =
> 无限烧钱跑到底。AWS Budgets/pytest maxfail 早停思想。

## Destination

EvalRunner `setRunBudgetChars(long)`（0=关默认）：逐项 input+expected
字符估算累计（run 内共享 AtomicLong——并行软上限 best-effort），超上限
→ 该项起剩余项不执行、记 error 三态 detail [RUN-BUDGET]（errored 计数
——三态语义不破坏）、run 照常落盘/事件（partial 显式不冒充完整）。
诚实边界：字符估算非精确 token（TokenEstimator SPI 可插拔后续）；软上限
（并行竞态容忍）；不退项。

## Notes

- 号段：spec 520 / T791–T792 / impl-423。

## Out of scope

- TokenEstimator 精确口径；per-item 预算；自动补跑。

## Tickets

- [x] [T791 预算记账与早停](../tickets/T791-eval-run-budget.md)
- [x] [T792 跳过项三态语义](../tickets/T792-eval-budget-skip.md)
