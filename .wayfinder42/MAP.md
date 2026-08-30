# Wayfinder Map — Buzhou run 对比 diff（effort #42，50 轮自迭代第 7 轮）

> effort #42，延续 #41（T313–T314 / impl-227）。主线：评估闭环有历史 run 落盘
> （spec 52/74）但无对比面——「改了 prompt 后哪些项由绿变红」要宿主自己拉两份
> JSON 手工对；LangSmith run compare / Promptfoo trend 是成熟形态。

## Destination

`EvalRunDiff.diff(base, head)` 纯函数：同 itemId 四态迁移（REGRESSION/FIX/
STABLE_PASS/STABLE_FAIL——fail↔error 同为红态不细分）+ 单侧项（数据集漂移）
单独计数 + netDelta（fixes-regressions）+ 项序确定；`runOf` 便捷构造（测试/宿主
拼装）；不触 store（输入面 = 回读的 EvalRunResult）。

## Notes

- 借鉴：LangSmith run compare / Promptfoo trend diff。

## Decisions so far

- 红态内部 fail↔error 不细分（对比语义关心红绿，不关心红的理由）。
- 单侧项不进四态（数据集漂移是独立信号，混入会污染回归计数）。

## Not yet specified

- diff 落盘/事件；A/B diff（abRun 明细对比）；趋势序列（多 run 折线）。

## Out of scope

- 沿用 #7–#41；UI 可视化（dashboard 模块另有面）。

## Tickets

- [x] [T317 EvalRunDiff 纯函数面 + runOf 构造](tickets/T317-run-diff.md)（impl-228）
- [x] [T318 4 例红队（四态/单侧/计数一致/项序）+ 文档收口](tickets/T318-run-diff-close.md)
