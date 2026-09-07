# Spec 80 — 评估回归门 EvalGate（effort #41）

> wayfinder map：`.wayfinder/maps/effort-41.md`（T313–T314）。借鉴：Promptfoo eval CI gate
> （`--eval-min-pass-rate`）/ LangSmith eval-as-gate。

## Problem Statement

评估闭环（spec 52/61/68）有数据集、runner、落盘、查询，但 CI 收口缺一步：宿主要
自己写「跑 run → 算 passRate → 比阈值 → 判红绿 + 拼失败摘要」的胶水，每家重复
且口径漂移（error 是否入分母、预览怎么截）。

## Solution

`EvalGate.enforce(datasetName, evaluator, threshold[, parallelism])` → `GateResult`：
passed（passRate ≥ threshold，error 计入分母从严）+ runId/三态计数 + 失败项预览
（itemId [状态] detail 首行，截 10 条 + 省略行）+ CI 单行 `summary()`。执行复用
EvalRunner（落盘/事件/注册表/并行语义不重复）；threshold clamp 0..1。

## User Stories

1. 作为 CI 作者，我要一行判定 + 一行摘要，所以 eval 门进 pipeline 零胶水。
2. 作为质量负责人，我要 error 计入分母，所以模型/基建故障不能伪装成绿。
3. 作为排查者，我要失败项预览，所以红了先看摘要不用查 store。

## Implementation Decisions

- 判定面与执行面分离（gate 不重复 runner 的落盘/事件——单一事实源）。
- 预览截 10 条（`PREVIEW_LIMIT`）+ 末尾省略计数行。
- summary 是人读面；exit-code 映射归宿主 CI 语义。

## Testing Decisions

- 8/2 过 0.8 门（含等值边界）+ OK 摘要字段；5/5 不过 0.9 门 + 5 条预览；
- 单项 error：0.5 压线过、0.51 不过（error 从严可证）+ [error] 预览；
- 15 失败截断（10 + 省略行）；threshold -0.5/2.0 clamp 至 0/1。

## Out of Scope

- exit-code 绑定；A/B 胜率门；门结果落盘（run 已落，gate 是视图）。

## Further Notes

- 与黄金轨迹（spec 32）互补：轨迹断言行为序列，gate 断言质量水位。
