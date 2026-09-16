# Spec 2018 — 检索结果强度重排器（effort #2018，R19）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3137–T3138，impl 1569）。
> 借鉴：mem0 思想的管线落地——spec 2003 的 MemoryStrengthScore 接进
> recall 排序面。

## Problem Statement

recall 召回只按相关度排序：同相关度下，30 天前的旧冷记忆与新近热访
高重要记忆无差别——检索质量被「相关但陈旧」稀释；而强度信息散在台账
侧，管线无从消费。

## Solution

`RecallStrengthReranker`（buzhou-memory recall，纯函数零状态）：

- `rerank(hits, metaOf, weights, relevanceWeight)`：finalScore =
  relevanceWeight × hit.score + (1−relevanceWeight) ×
  MemoryStrengthScore.score(meta)——默认 0.7 相关度为主，强度微调
  同分段次序；
- 开闭装饰（不侵入 RecallSearch 本体）；元数据由调用方逐 Hit 供给
  （metaOf 返 null = 零强度垫底——旧冷降权）；
- TIME 模式（score 恒 1）同样适用——退化为纯强度序；
- 排序稳定（同融合分保原相关度序）；契约：hits 非 null、
  relevanceWeight ∈ [0,1] fail-fast。

## User Stories

1. 作为 recall 作者，同相关度下新热记忆靠前——「相关且新鲜」优先。
2. 作为调参者，relevanceWeight 两极（1 纯相关度 / 0 纯强度）可退化
   ——场景可偏置。

## Testing Decisions

- 同相关度新热靠前；高相关低强度仍胜低相关满强度（0.7 口径算术钉
  死）；纯强度/纯相关度两极退化；null 元数据垫底；同分稳定保序；
  TIME 模式强度序；畸形三型 fail-fast。

## Out of Scope

- 不做强度写回台账（访问计数维护归调用方）；不接 RecallSearchTool
  装配（端到端接线归后续轮）。

## Further Notes

- 与 MultiQueryRetriever（多查询扩展召回）正交：那扩召回面，这精排
  序面。
