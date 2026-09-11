# Spec 513 — 评估 A/A 抖动检测（effort #513）

> wayfinder map：`.wayfinder/maps/effort-513.md`（T777–T778）。E 会话第 14 轮。

## Problem Statement

评估闭环有 run 落盘/对比/门/指纹——A/A 检测（同数据集同版本跑两遍，
逐项 verdict 翻转 = 抖动项）无命名语义：EvalRunDiff 的 REGRESSION/FIX
方向语义在 A/A 语境不存在（两次相同配置不该有方向），抖动项被误读成
回归/修复。先测评估系统自身稳定性再谈版本对比（A/A test）。

## Solution

`eval.EvalFlakinessDetector`（纯函数，EvalRunDiff 同型）：

- `analyze(runA, runB)` → `FlakinessReport(runAId, runBId, compared,
  flakyItems, driftItems, flakyRate)`。
- 红绿映射：pass=绿、fail/error=红（321 错误从严——error 不折算 pass）；
  **抖动** = 同项红绿翻转（方向不区分）；单侧项（仅一侧存在）= 数据集
  漂移 driftItems，不进抖动分母。
- `flakyRate` = flaky / compared（compared=双侧都在的项数；0 项约定 0）。
- 消费：host 同指纹数据集两 run → analyze → 抖动清单标记/隔离；与
  EvalGate 组合宿主一行（抖动率超阈 fail）。

## User Stories

1. 作为评测方，我想跑 A/A 检测评估系统自身稳定性， so 抖动项先被标记
   （quarantine）而不污染版本对比结论。
2. 作为宿主，我想区分「数据集漂移」与「真抖动」， so 单侧项不计入
   抖动率。

## Implementation Decisions

- 红绿二值化（fail/error 同红）——与 81 STABLE_FAIL 内部不细分同口径。
- 纯函数不触 store（输入可从 EvalQueryService 回读）。

## Testing Decisions

- 两 run 全同 → flakyRate 0；单项翻转 → flaky=1（statusA/statusB 记录）；
  pass↔error 与 pass↔fail 同判抖动；fail↔error 同红不抖动。
- 单侧项 → driftItems 不进分母；空 run；flakyRate 数学。

## Out of Scope

- k 次重复编排；duration 方差；自动 quarantine。

## Further Notes

- 新公共类型 `EvalFlakinessDetector`（嵌套 `FlakinessReport`/
  `FlakyItem`）随轮 regenerate 快照 + api-surface.md 加行。
