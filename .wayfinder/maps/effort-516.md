# Wayfinder Map — Buzhou judge 校准跟踪（effort #516，E 会话第 17 轮）

> E 会话第 17 轮。勘察：LLM-as-judge（61）产出 PASS/FAIL verdict，
> A/B 对比（71）与门（80）都**信任 judge**——judge 自身与金标准（人工
> 标注/期望断言）的一致率无计量面：judge 系统性偏差（把 error 判 pass、
> 宽松倾向）直接污染全部下游结论。LightEval/工业 judge calibration 思想。

## Destination

`eval.JudgeCalibration`（纯函数——513/81 同型）：`calibrate(golden,
judged)` → CalibrationReport（TP/TN/FP/FN/agreement/precision/recall/
f1/singleSided）。二值化：pass=判绿、fail/error=判红（judge 端）；
金标准端=期望断言（expected pass/fail）。判红为「正类」（judge 的价值
在抓坏——precision/recall 以判红为轴）。单侧项排除（漂移非偏差——513
同口径）。0 除约定：分母 0 → 该指标 null（诚实空值非 0——416 同口径）。

## Notes

- 号段：spec 516 / T783–T784 / impl-419。
- 借鉴源：LightEval / 工业 judge calibration（混淆矩阵四率）。
- 诚实边界：golden 须为断言型（非第二个 judge——双 judge 一致率是另一
  语义）；仅二值（judge 评分制不做）。

## Out of scope

- 评分制 judge；多 judge 一致率；自动阈值校准。

## Tickets

- [x] [T783 混淆矩阵四率](../tickets/T783-judge-calibration.md)
- [x] [T784 单侧排除与空值口径](../tickets/T784-judge-calibration-report.md)
