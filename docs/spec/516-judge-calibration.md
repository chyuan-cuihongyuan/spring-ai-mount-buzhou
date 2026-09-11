# Spec 516 — judge 校准跟踪（effort #516）

> wayfinder map：`.wayfinder/maps/effort-516.md`（T783–T784）。E 会话第 17 轮。

## Problem Statement

LLM-as-judge（61）的 verdict 被 A/B（71）与门（80）直接信任——judge
自身与金标准的一致率无计量：系统性偏差（宽松倾向/error 误判 pass）静默
污染全部下游结论。校准需先于信任（混淆矩阵四率）。

## Solution

`eval.JudgeCalibration`（纯函数，513 同型）：

- `calibrate(golden, judged)` → `CalibrationReport(tp, tn, fp, fn,
  singleSided, agreement, precision, recall, f1)`。
- 二值化：判红 = fail|error（judge 端与金标准端同映射）；**判红为正类**
  （judge 的价值在抓坏）。
- 指标：agreement=(tp+tn)/n；precision=tp/(tp+fp)；recall=tp/(tp+fn)；
  f1 调和平均。分母 0 → null（诚实空值非 0——416 同口径）。
- 单侧项（仅一侧存在）排除计数 singleSided（漂移非偏差——513 同口径）。

## User Stories

1. 作为评测方，我想在信任 judge 前先对金标准集校准， so judge 的宽松
   倾向（FP 低 precision）先于版本对比被发现。
2. 作为运维，我想校准面持久可查， so judge 换版后一致性漂移可跟踪。

## Implementation Decisions

- 判红为正类（抓坏是 judge 价值轴）；golden 须为断言型（双 judge 一致率
  是另一语义，不混）。
- 纯函数不触 store；输入从 EvalQueryService 回读。

## Testing Decisions

- 完全一致 → agreement 1；构造 FP/FN 各一 → precision/recall 数学断言；
  单侧排除；空分母 → null 指标；null run fail-fast。

## Out of Scope

- 评分制；多 judge；自动校准。

## Further Notes

- 新公共类型 `JudgeCalibration`（嵌套 `CalibrationReport`）随轮
  regenerate 快照 + api-surface.md 加行。
