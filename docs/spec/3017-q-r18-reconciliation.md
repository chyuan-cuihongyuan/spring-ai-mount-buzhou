# Spec 3017 — Q 会话 R18 对账轮（effort #3017，R18）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5035–T5036，impl 2018）。
> R6k 对账轮第三例（Wave 3 收口）。

## Problem Statement

Wave 3（R13–R17）新增 5 个公共类型（TopologicalSorter /
SweepLineIntervals / KmpSearch / FenwickTree / BatchAccumulator）
未入快照——快照门在全仓 verify 必红。

## Solution

R6k 同款四件套：快照 1065→1070（+5 全 Q 系 Wave 3，reactor 全量
regenerate）+ api-surface.md 五行（concurrent×2 / metrics×3 两段落
位）+ CONTEXT 964→969 + 全仓 mvn verify 三门绿 + push。

## Further Notes

- 里程碑：18/150（12%）。Wave 3 零修复（Fenwick 整除截断期望值
  由 rc 门禁在提交前拦截修正）。
