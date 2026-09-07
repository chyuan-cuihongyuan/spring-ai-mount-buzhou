# Spec 198 — 门禁宽松档（effort #143）

> wayfinder map：`.wayfinder/maps/effort-143.md`（T562–T563）。spec 150 fog「宽严
> 两档」收口。

## Solution

`EvalRunner.setExpectations(suite, warnOnly)`：宽松档未过只 WARN（带 summary +
前三条发现——门禁日志与严格档同明细）不拦，灰度期「看到脏但照跑」；单参
`setExpectations(suite)` 保持严格档（既有零变化）。两档共享同一 validate 面。

## Testing Decisions

- 红队：宽松档重复输入照跑 2 项；严格档同数据集仍拦（zero 变化）。
