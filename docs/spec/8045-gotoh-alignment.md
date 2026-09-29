# Spec 8045 — GotohAlignment（effort #8045，V46）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8091–V8092，impl 2347）。
> 借鉴：Gotoh 1982 仿射间隙思想。

## Problem Statement

线性间隙的病：连续缺口按位罚（生物间隙一次事件）——**M/A/B 三矩阵仿射间隙 DP**。

## Solution

GotohAlignment（core/eval）：of(match,mismatch,gapOpen,gapExtend)+score（间隙开口+延伸）；gapOpen=gapExtend 退化线性与 NW 圣像全等；fail-fast+确定性。

## Testing Decisions

线性退化 vs NW 全等+仿射手锚（长间隙惩罚体现）+fail-fast。

## Out of Scope

- 不做多序列/带约束比对（成对面明示）。

## Further Notes

- 里程碑：V46/50。
