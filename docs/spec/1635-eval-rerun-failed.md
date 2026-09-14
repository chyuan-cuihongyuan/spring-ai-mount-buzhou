# 1635 · eval 失败项重跑（rerun-failed）

> 来源：N 会话 R36（effort #1635 / T2421–T2422 / impl 1188）。

## Solution

`EvalRunner.run(datasetName, evaluator, parallelism, onlyItemIds)` 4 参重载：
onlyItemIds 非 null 时 items 过滤为子集（id 命中）；汇总/落盘/事件口径不变
（total=子集数、新 runId）。宿主把上轮 fail+error 项的 id 传入即 rerun-failed。

## Testing Decisions

- `EvalRerunFailedTest` 两断言：全量跑（3 项 1 败）→ 失败 id 子集重跑
  （total=1、新 runId、单项）；null 子集全量零变化。
- 回归：eval 包全量 245 用例。

## Out of Scope

- rerun 的自动串联（失败→重跑→对比报告一条命令——编排面归宿主脚本）。
- flaky 判定阈值（多次重跑统计——EvalFlakinessDetector 域）。
