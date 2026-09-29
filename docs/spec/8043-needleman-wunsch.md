# Spec 8043 — NeedlemanWunsch（effort #8043，V43）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8087–V8088，impl 2345）。
> 借鉴：Needleman & Wunsch 1970 全局比对思想。

## Problem Statement

序列比对的病：全比对路径枚举指数——**全矩阵 DP + 回溯**得最优全局比对得分与比对路径。

## Solution

NeedlemanWunsch（core/metrics）：of(match,mismatch,gap)+align 返回 score 与比对串对（回溯并列取上——canonical）+null/空串（空对空合法 0）fail-fast+确定性。

## Testing Decisions

经典小例手锚+300 随机小串 vs 记忆化递归圣像得分全等+并列 canonical+fail-fast。

## Out of Scope

- 不做多序列/带约束比对（成对面明示）。

## Further Notes

- 里程碑：V43/50。
