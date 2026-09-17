# Spec 3035 — Q 会话 R36 对账轮（effort #3035，R36）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5071–T5072，impl 2036）。
> R6k 对账轮第六例（Wave 6 收口）。

## Problem Statement

Wave 6（R31–R35）新增 5 个公共类型（HashedWheelTimers /
VirtualRuntimeQueue / MaglevHash / TwoQueueCache / CrostonForecaster）
未入快照——快照门在全仓 verify 必红。

## Solution

R6k 同款四件套：快照 1080→1085（+5 全 Q 系 Wave 6，reactor 全量
regenerate）+ api-surface.md 五行（concurrent×2 / cache / policy /
metrics 四段落位）+ CONTEXT 979→984 + 全仓 mvn verify 三门绿 +
push。

## Further Notes

- 里程碑：36/150（24%）。Wave 6 占坑换题四例（标签基数限制/
  负缓存/Misra-Gries/HRW 均已被 605/1616/1848/1862 占用）——grep
  夘核纪律第六波连续生效；时间轮槽重访语义与 2Q mainCapacity
  耦合两教训均提交前拦截修正。
