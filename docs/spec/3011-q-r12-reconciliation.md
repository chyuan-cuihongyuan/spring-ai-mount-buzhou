# Spec 3011 — Q 会话 R12 对账轮（effort #3011，R12）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5023–T5024，impl 2012）。
> R6k 对账轮第二例（Wave 2 收口）。

## Problem Statement

Wave 2（R7–R11）新增 5 个公共类型（PSquareQuantile /
HybridLogicalClock / MorrisCounter / AdaptiveReplacementCache /
MinHashSketch）未入快照——快照门在全仓 verify 必红。

## Solution

R6k 同款四件套：快照 1060→1065（+5 全 Q 系 Wave 2，reactor 全量
regenerate）+ api-surface.md 五行（cache/concurrent/metrics 三段
落位——cache 包首入 Q 系）+ CONTEXT 959→964 + 全仓 mvn verify
三门绿 + push。

## Further Notes

- 里程碑：12/150（8%）。Wave 2 含两处收尾修复（ARC |L1|≤c 不变量
  与幽灵手迹精算、HLC 测试缺 import）——均 rc 门禁拦截后修复，
  未流出红提交。
