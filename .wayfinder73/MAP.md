# Wayfinder Map — Buzhou 评估 run 时长 timer（effort #73，50 轮自迭代第 38 轮）

> effort #73，延续 #72（T403–T404 / impl-257）。主线：评估面只有 per-item
> durationMs（落盘明细）——整跑时长（数据集规模感知的时长告警、评估风暴检测）
> 无指标维。

## Destination

双 timer（BuzhouMetricsHolder）：EvalRunner 完成点
`buzhou.eval.run.duration`（startedAt→finishedAt 既有值复用）+
PairwiseEvalRunner 完成点 `buzhou.eval.ab-run.duration`。执行/落盘/事件零变化。

## Notes

- 借鉴：spec 70 家族 timer 纪律；LangSmith run latency 观测面。

## Decisions so far

- 完成点计时（不包 try-finally——run 内异常已收敛为 item error，整跑恒达完成点）。

## Not yet specified

- 按 dataset 维度时长（dataset 名不可进 tag——无界；进程内表另议）。

## Out of scope

- 沿用 #7–#72。

## Tickets

- [x] [T405 eval/ab 双 run duration timer](tickets/T407-eval-timer.md)（impl-258）
- [x] [T406 红队（双 timer 各一次 + 非负）+ 收口](tickets/T408-eval-timer-close.md)
