# Spec 1735 — 技能排序一致性读面（effort #1735，R36）（effort #1735，R36）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2671–T2672，impl 1335，impl scikit-learn Kendall τ / 排序一致性评估）。借鉴：词法与语义双排序器的排序分歧无量化：τ 一致 → 双路冗余可简化；分歧大 → 语义路在贡献真信号。

## Problem Statement

`SkillRankAgreement`（skill，静态纯函数）：tau(rankA, rankB) 在公共项上算 Kendall τ=(C−D)/(C+D)，公共项<2 哨兵 −1；值域 [−1,1]；Agreement(commonItems/concordant/discordant/tau)。

## Solution

作为架构简化者，τ 恒 0.95 → 词法路可降级。

## User Stories

1. 17350
2. 17351
3. 17352

## Implementation Decisions

- 17353

## Testing Decisions

- 17354

## Out of Scope

- 17355

## Further Notes

- 17356
