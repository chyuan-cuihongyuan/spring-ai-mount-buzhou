# Spec 505 — 在线实验分桶（effort #505）

> wayfinder map：`.wayfinder/maps/effort-505.md`（T761–T762）。E 会话第 6 轮。

## Problem Statement

评测 A/B（31/71/75）是离线 runner；在线「session→variant 确定性分配 +
曝光统计」空白。宿主想对线上流量做提示词/模型/参数对照时，需要自己写
哈希分桶、自己记曝光——每家造一遍轮子且口径不一。

## Solution

`core.experiment.ExperimentBucketer`（原语先行，411/415 同族；
GrowthBook/Statsig 思想）：

- **yml**：`buzhou.experiments.<experiment>.<variant> = 权重（整数百分
  比）`；Binder 预绑 map 非空才装配（409 同法）。
- **assign(experiment, unitKey) → String|null**：sha256(experiment|unit)
  低 32 位 floorMod 100 落桶 → 变体名（声明序）累积权重命中；跨实例
  零协调天然一致（415 同思想）；权重和 ≤100、余量=null 未入组；
  未知实验=null 且零状态（有界）。
- **曝光统计**：experiment×variant 有界计数（仅已声明变体）+ snapshot()
  + `buzhou.experiment.assigned` counter（tag experiment/variant 有界
  ——声明驱动）。
- **消费面归宿主**：按 variant 选提示词/模型/参数；框架只管确定性分配
  与曝光可见，效果显著性计算归外部 OLAP。

## User Stories

1. 作为宿主，我想声明「prompt-v2 实验：control 50 / treatment 50」，
   so 每个会话确定性地落在某组且重启/换实例不翻身。
2. 作为产品，我想看到各变体曝光计数，so 流量切分是否符合预期一屏可见。

## Implementation Decisions

- 哈希含实验名——同一 unit 在不同实验独立随机（实验间不相关）。
- 权重和 >100 或负值 → 启动 fail-fast（桶只有 100 个）。
- 变体遍历按名字典序（确定性跨实例，不依赖 yml 声明序）。

## Testing Decisions

- 确定性：同 key 重复 assign 恒同 variant；分布健全性：1000 键全变体
  有命中（宽容差）；权重和 50 → 约 50% 未入组（宽容差）。
- 未知实验 null；权重和超 100 装配 fail-fast；yml map 非空装配/缺席无。

## Out of Scope

- 显著性计算；分层实验；强制改组 API；自动放量。

## Further Notes

- 新公共类型 `ExperimentBucketer`、`BuzhouExperimentProperties` 随轮
  regenerate 快照 + api-surface.md 加行。
