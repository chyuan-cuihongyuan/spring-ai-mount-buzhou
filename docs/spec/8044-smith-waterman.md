# Spec 8044 — SmithWaterman（effort #8044，V45）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8089–V8090，impl 2346）。
> 借鉴：Smith & Waterman 1981 局部比对思想。

## Problem Statement

局部相似域的病：全局比对强设端到端——**负值归零的局部 DP + 从最大值回溯到 0**得最优局部子串对。

## Solution

SmithWaterman（core/metrics）：of(match,mismatch,gap)+align 返回 bestScore 与局部比对子串对（全负得 0 空比对诚实）+fail-fast+确定性。

## Testing Decisions

手锚（局部高相似段嵌长噪声）；300 随机 vs 全子串对枚举 NW 圣像 max 全等；fail-fast。

## Out of Scope

- 不做多序列/带约束比对（成对面明示）。

## Further Notes

- 里程碑：V45/50。
