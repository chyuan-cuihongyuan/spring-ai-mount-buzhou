# Wayfinder Map — Buzhou 时间桶延迟分位数（effort #416，D 会话第 17 轮）

> D 会话第 17 轮（#412 扩散轮——C 会话 336 扩散 333 同节奏）。勘察：
> 412 的 TimeBucket 有 counts/tokens/errors 但**无时延面**——「错误率正常
> 但变慢了」这种最隐蔽的退化不可见。Prometheus histogram_quantile /
> SRE latency SLI 的 p50/p95/p99 缺失。

## Destination

`DashboardQueryService.TimeBucket` 扩散增 `turnP50Ms/turnP95Ms/
turnP99Ms`（桶内 TURN span 时延分位数——exact 算法：收集→排序→
最近秩取值；桶内样本可数百，exact 无需 HD/tdigest 依赖）；零样本桶
分位数为 null（诚实空值——不是 0）。HTTP 自动携带（记录字段直出）。
快照零 diff（嵌套记录不增类型）仍 regenerate 验证。

## Notes

- 号段：spec 416 / T723–T724 / impl-389。
- 借鉴源：Prometheus histogram_quantile + Google SRE latency SLI。
- 纪律：exact 分位数（dashboard 规模不引入 sketch 依赖——诚实边界：
  超大桶耗时线性，桶粒度纪律已限样本量）；pXX 最近秩（nearest rank）。

## Out of scope

- tdigest/HD sketch；模型/工具级分位（扩散候选）；SLA 阈值判定
（312 族消费）；流式分位。

## Tickets

- [x] [T723 分位数聚合](../tickets/T723-rollup-percentiles.md)
- [x] [T724 空桶诚实空值](../tickets/T724-percentile-null.md)
