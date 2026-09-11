# Wayfinder Map — Buzhou 在线实验分桶（effort #505，E 会话第 6 轮）

> E 会话第 6 轮。勘察：评测面 A/B（31/71/75 系）是**离线 runner**——
> 数据集上跑两版本比胜率；**在线**「同一实验下 session→variant 确定性
> 分配 + 曝光统计」空白（415 亲和键是路由提示不是实验分桶）。412 哈希
> 确定性采样同族思想但语义不同（407/423 是采样入集，本轮是变体分配）。
> GrowthBook/Statsig 实验分桶思想。

## Destination

`core.experiment.ExperimentBucketer`（原语先行——411/415 同族）：
yml `buzhou.experiments.<experiment>.<variant> = 权重（百分比）`——
`assign(experiment, unitKey)` 确定性分桶：sha256(experiment|unitKey) 低
32 位 floorMod 100 落桶 → 有序累积权重命中 variant（跨实例零协调天然
一致——415 同思想）；权重和 ≤100、余量=未入组（null，GrowthBook 同
语义）；未知实验=null 零状态。曝光统计：experiment×variant 有界计数
（只数已声明变体）+ snapshot() + MetricsHolder counter（tag 有界）。
`BuzhouExperimentProperties`（Binder 预绑 map 非空才装配——409 同法）。
宿主消费面：注入 bucketer 按 variant 选提示词/模型/参数——决策面归宿主，
框架只管确定性分配与曝光可见。

## Notes

- 号段：spec 505 / T761–T762 / impl-408。
- 借鉴源：GrowthBook / Statsig 实验分桶（确定性哈希+权重累积+未入组余量）。
- 诚实边界：框架不做效果显著性计算（统计归外部 OLAP——曝光数据经
  snapshot/counter 出）；hash 不加盐跨实验独立（同 unit 不同实验可
  不同组——实验间独立随机）。

## Out of scope

- 显著性/效果计算；分层实验（layers）；粘性覆盖 API（改组强制）；
  逐步放量自动化（可经 340 热载权重曲线手动实现）。

## Tickets

- [x] [T761 确定性分桶原语](../tickets/T761-experiment-bucketer.md)
- [x] [T762 曝光统计与装配](../tickets/T762-experiment-assembly.md)
