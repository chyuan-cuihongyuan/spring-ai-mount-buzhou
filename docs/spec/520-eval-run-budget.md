# Spec 520 — 评估 run 预算闸（effort #520）

> wayfinder map：`.wayfinder/maps/effort-520.md`（T791–T792）。E 会话第 21 轮。

## Problem Statement

eval run 逐项真调模型——run 无预算上限：坏 judge/大面积失败的数据集 =
无限烧钱跑到底。会话面预算（16/338）不覆盖 eval 面。

## Solution

EvalRunner `setRunBudgetChars(long)`（0=关，默认零变化）：

- 估算口径：逐项 `input.length() + expected.length()` 字符累计（执行前
  记账；TokenEstimator 精确口径留 SPI 扩散）。
- 累计估算超上限 → 该项起全部剩余项不执行，记 error 三态（detail
  `[RUN-BUDGET] … 未执行`，errored 计数）——run 照常完成落盘/发事件
  （partial 显式可见，消费者语义不变）。
- 共享 AtomicLong（并行下软上限——best-effort 竞态容忍；68 并行语义
  不破坏：结果仍按项序聚合）。

## User Stories

1. 作为评测方，我想给 run 设字符预算， so 坏 judge/死循环数据集早停
   而非无限烧钱。
2. 作为运维，我想跳过项以 error 三态显式可见， so partial run 不冒充
   完整 run。

## Implementation Decisions

- 超限后全部跳过（不退项重试——partial 显式）。
- 跳过记 STATUS_ERROR（三态语义不变，不引入第四态破坏消费者）。
- 预算 0 = 关（零默认行为变化）。

## Testing Decisions

- 预算覆盖前 2 项：前 2 pass、其余 [RUN-BUDGET] error；errored 计数对。
- 无预算（默认）全执行零变化；并行度 >1 下预算软生效（不精确断言）。

## Out of Scope

- TokenEstimator 精确口径；per-item 预算；自动补跑。

## Further Notes

- 无新顶层公共类型（EvalRunner 加法变更）——快照零 diff 预期。
