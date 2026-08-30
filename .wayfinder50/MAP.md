# Wayfinder Map — Buzhou G-Eval 自定义维度打分（effort #50，50 轮自迭代第 15 轮）

> effort #50，延续 #49（T339–T340 / impl-235）。主线：数值评估器（spec 87）只有
> 两个预置指标——「合规性/语气/精度/简洁性」等领域口径每个都要写子类；DeepEval
> G-Eval 的内核 = 维度名 + rubric + 分值。

## Destination

`RagasEvaluators.gEval(judge, dimension, rubric[, threshold=0.8])`：自定义维度
0-10 打分（detail 前缀 [dimension]——看板可按维度分组）；参照系 BOTH（输入与
黄金答案都进 prompt——自定义维度各取所需）；维度名空 = IllegalArgumentException；
顺带重构：ScoredJudge 参照系由字符串嗅探改 Reference 枚举（EXPECTED/INPUT/BOTH，
测试钉住）。

## Notes

- 借鉴：DeepEval G-Eval（custom criteria + score）；与 spec 87 协议/异常面完全共用。

## Decisions so far

- 维度名进 detail 前缀（OLAP/看板按维度分组的溯源锚点，零 schema 成本）。

## Not yet specified

- 多维度组合（一次 run 多 gEval 并行打分——runner 组合面已有，聚合视图另议）。

## Out of scope

- 沿用 #7–#49；CoT 步骤显式化（G-Eval 原论文的 thought 步骤——judge 模型自担）。

## Tickets

- [x] [T341 gEval 工厂 + Reference 参照系重构](tickets/T341-geval.md)（impl-236）
- [x] [T342 gEval 红队 + 既有 spec87 红队回归 + 收口](tickets/T342-geval-close.md)
