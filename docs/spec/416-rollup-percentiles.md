# Spec 416 — 时间桶延迟分位数（effort #416）

> wayfinder map：`.wayfinder/maps/effort-416.md`（T723–T724）。D 会话第 17 轮。

## Problem Statement

时间桶预聚合（412）有 counts/tokens/errors 无时延面——「错误率正常但
变慢了」的最隐蔽退化不可见；时延趋势要导出 span 离线算。

## Solution

`TimeBucket` 扩散（Prometheus histogram_quantile / SRE latency SLI 借鉴）：

- 增 `turnP50Ms` / `turnP95Ms` / `turnP99Ms`（record 字段 Integer 可空）：
  桶内 TURN span 时延（durationMs 同 stats 口径）exact 最近秩分位
  （nearest rank：排序后取 ceil(p*n)-1 位）。
- 零样本桶分位数 = **null**（诚实空值——不是 0；JSON null）。
- exact 算法（不引入 sketch 依赖——dashboard 规模桶内样本有限，桶粒度
  纪律已限总量；超大桶线性耗时的诚实边界文档化）。
- HTTP /api/rollups 自动携带（记录字段直出）；既有消费方新增字段为
  加法兼容。

## User Stories

1. 作为 SRE，我想小时级 p95 时延趋势一查询即得，so 「变慢但没报错」
   的退化可见。
2. 作为图表作者，我想空桶分位是 null 而不是 0，so 图表不把无数据
   画成零延迟假象。

## Implementation Decisions

- nearest rank（业界简明口径，可离线复算验证）。

## Testing Decisions

- 数值驱动分位（已知时长集合验证 p50/p95/p99）；零样本桶 null；
  混合桶（有 TURN 的桶出分位、空桶 null）。

## Out of Scope

- sketch；维度分位；SLA 判定；流式。

## Further Notes

- 嵌套记录字段扩展不增公共类型（快照零 diff 预判——仍 regenerate）。
