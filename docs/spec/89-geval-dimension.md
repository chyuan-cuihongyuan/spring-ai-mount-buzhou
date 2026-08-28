# Spec 89 — G-Eval 自定义维度打分（effort #50）

> wayfinder map：`.wayfinder50/MAP.md`（T341–T342）。借鉴：DeepEval G-Eval。

## Problem Statement

数值评估器（spec 87）预置 faithfulness/answerRelevancy 两指标；「合规性/语气/
精度/简洁性」等领域口径每个都要写子类——评分标准本该是数据不是代码。

## Solution

`RagasEvaluators.gEval(judge, dimension, rubric[, threshold=0.8])`：维度名 +
rubric 描述进 prompt，judge 打 0-10（S x/y 协议沿用）；detail 前缀
`[dimension]`（看板/OLAP 按维度分组）；参照系 BOTH（输入与黄金答案都进 prompt）；
维度名空 = IllegalArgumentException（fail-fast——detail 溯源锚点必须有）。
内部重构：ScoredJudge 参照系从字符串嗅探改 Reference 枚举（EXPECTED/INPUT/BOTH）。

## User Stories

1. 作为评估作者，我要一行定义领域维度，所以评分口径不进代码库。
2. 作为看板作者，我要 detail 带维度名，所以多维度 run 可分组聚合。

## Implementation Decisions

- 与 spec 87 共用协议/钳制/异常面（gEval 是工厂不是新内核）。
- rubric 空 = 泛化标准（0-10 质量）——不强制（领域口径可极简）。

## Testing Decisions

- gEval：0.7 过 0.6 门 + detail 前缀 + BOTH 参照（prompt 含输入/黄金答案/维度名）；
  空维度名 fail-fast；spec 87 既有四例回归（重构不漂移）。

## Out of Scope

- 多维度组合聚合视图；CoT 显式步骤。

## Further Notes

- 维度名 = OLAP 分组键（与 spec 88 导出配合：detail 解析即维度列）。
