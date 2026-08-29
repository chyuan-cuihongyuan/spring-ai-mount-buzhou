# Wayfinder Map — Buzhou 工具调用时长 timer（effort #70，50 轮自迭代第 35 轮）

> effort #70，延续 #69（T393–T394 / impl-254）。主线：工具面只有调用计数
>（buzhou.tool.calls outcome）——「哪个工具慢」不可聚合：慢工具检测（P95 告警）
> 缺 timer 维。

## Destination

HookedToolCallback 在 delegate.call 外围计时：timer `buzhou.tool.duration`
（tag outcome=ok|failed 有界——慢工具与失败工具延迟可分）；既有 counter/错误反馈
通道零变化；全工具统一（所有机制的工具都经本回调——既有先例注释沿用）。

## Notes

- 借鉴：Micrometer timer + Prometheus histogram 语义（P95 慢工具可告警）。

## Decisions so far

- 计时覆盖 delegate 调用本体（不含 hook 链——hook 链延迟归属另议）。

## Not yet specified

- per-tool 维度（工具名不可进 tag——无界；进程内 per-tool 表另议）。

## Out of scope

- 沿用 #7–#69。

## Tickets

- [x] [T397 tool.duration timer 接线](tickets/T399-tool-timer.md)（impl-255）
- [x] [T398 红队（ok/failed 双计时 + counter 回归）+ 收口](tickets/T400-tool-timer-close.md)
