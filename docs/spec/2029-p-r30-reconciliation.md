# Spec 2029 — P 会话 R30 对账轮（effort #2029，R30）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3159–T3160，impl 1580）。
> R6k 对账轮第五例（Wave 5 收口，1/5 里程碑）。

## Problem Statement

Wave 5（R25–R29）新增 5 个公共类型（AgingPriorityQueue /
ConsistentHashRing / WeightedFairScheduler / EwmaEstimator /
ConcurrencyGroupGate）未入快照；R19 起网络波动积压提交待推。

## Solution

R6k 同款四件套：快照 1021→1026（+5 全 P 系）+ api-surface.md 五行 +
CONTEXT 920→925 + 全仓 mvn verify 三门全绿 + P 对账门核账（spec
2000–2029 卅号四件套）+ push 补推全部积压。

## Further Notes

- 里程碑：30/150（1/5）——调度原语族成谱系（aging/DRR/并发组/哈希环
  四件 + EWMA 平滑层）。
