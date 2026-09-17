# Spec 3023 — Q 会话 R24 对账轮（effort #3023，R24）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5047–T5048，impl 2024）。
> R6k 对账轮第四例（Wave 4 收口）。

## Problem Statement

Wave 4（R19–R23）新增 5 个公共类型（TarjanSccFinder / TtlJitter /
JumpConsistentHash / HoltForecaster / TwoChoiceSelector）未入快照
——快照门在全仓 verify 必红。

## Solution

R6k 同款四件套：快照 1070→1075（+5 全 Q 系 Wave 4，reactor 全量
regenerate）+ api-surface.md 五行（concurrent×2 / cache / metrics /
policy 四段落位）+ CONTEXT 969→974 + 全仓 mvn verify 三门绿 +
push。

## Further Notes

- 里程碑：24/150（16%）。Wave 4 占坑换题三例（RRF/BM25/Wilson/
  burn-rate 均已被 605/803/1630/321+817 占用）——grep 夘核纪律
  生效；概率口径教训（相异重抽 vs 带放回）与归一化常数教训
  （TtlJitter 2^62）均 rc 门禁提交前拦截。
