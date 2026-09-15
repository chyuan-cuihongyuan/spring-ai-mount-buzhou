# Spec 1133 — 预算钳位读面（effort #1133，J 会话 R113——工件由 L 会话 R49 吸收补登）

> wayfinder map：`.wayfinder/maps/effort-1000.md`（T1685–T1686，impl 871）。
> **补登说明**：J 会话 R113/R114（commit 44dd73cb/12534817）把 `BudgetClampStats`
> 代码与 README 纵深行合入 main，但本 spec 文件未随之入库（README 死链
> `docs/spec/1133-budgetclamp-stats.md` 致覆盖门红）——L 会话 1700 系 R49
> 收口审计发现后，按吸收补登纪律依 main 上的实际代码代写本文件。

## Problem Statement

DefaultBudgetCalculator 的预算判定只给瞬时结果：评估了多少次、负预算
（需求超窗口）被钳到 0 的频次如何——「钳位发生频次」是窗口/预留参数
是否失配的直接信号，无读数则调参盲航。

## Solution

`DefaultBudgetCalculator` 静态三计数 + R114 尾参两计数（spec 1134）：

- `evaluations` 预算评估总次数；
- `negativeClamps` 负预算钳位次数（effective 窗口为负仍诚实产出 0 预算）；
- `normalBudgets` 正常预算次数；
- `neededTrue/neededFalse`（R114 追加）压缩触发判定分布（压缩压力信号）；
- `stats()` 只读快照，守恒式 `evaluations = negativeClamps + normalBudgets`；
- `resetForTest()` 测试归零（生产禁用——计数器是进程生命周期水位）。

## User Stories

1. 作为预算调参者，negativeClamps 频发 → 窗口/预留失配，先修配置。
2. 作为压缩治理者，neededTrue 占比高 → 压缩触发压力信号（spec 1134 深化）。

## Implementation Decisions

- 进程级 AtomicLong 静态计数（水位语义）；evaluate 判定处落桶，返回值
  语义逐位不变（纯读面零行为变化）。

## Testing Decisions

- BudgetClampStatsTest：守恒断言 evaluations = 两桶之和；钳位用例以超大
  reserve 驱动负预算（TableContextWindowResolver 默认窗口量级适配）。

## Out of Scope

- 不改预算算法本体；不做按模型细分。

## Further Notes

- J 会话 R113 原始工件（票 T1685–T1686 / impl 871）在其会话分支；
  main 上的代码与 README 行为权威事实源，本文件为四面一致的补登件。
