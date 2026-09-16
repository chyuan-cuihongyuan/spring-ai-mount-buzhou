# Spec 2017 — P 会话 R18 对账轮（effort #2017，R18）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3135–T3136，impl 1568）。
> R6k 对账轮第三例（Wave 3 收口）。

## Problem Statement

Wave 3（R13–R17）新增 5 个公共类型（PreemptionLedger /
StartupGraceTracker / KeyCompaction / MinRttTracker / CuckooFilter）
未入快照；五轮工件链需机器核账。

## Solution

R6k 同款四件套：regenerateSnapshot 快照 1011→1016（+5 全 P 系）+
api-surface.md 五行 + CONTEXT 910→915 + 全仓 mvn verify 三门全绿 +
P 对账门核账（spec 2000–2017 十八号四件套）+ push。

## Testing Decisions

- verify 全绿即测试；对账门常驻复跑。

## Further Notes

- 插行锚点两次未命中（ToolPolicyMatchStats/SessionBloomFilter 段落
  錯位）——锚点容错补插已成本轮例行。
