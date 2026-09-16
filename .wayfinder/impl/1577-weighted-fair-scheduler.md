# impl 1577 — 加权公平调度器（spec 2026 / T3153–T3154 / R27）

纵切片：`WeightedFairScheduler`（core/exec 主）+
`WeightedFairSchedulerTest`（七用例）。粘性 DRR、长期公平、清账退休。

- 测试：`mvn -pl buzhou-core test -Dtest=WeightedFairSchedulerTest` 7/7 绿。
- 教训入档：单项出口 DRR 必须粘性轮内消费，否则 deficit 累积而权重
  失效（长期比 1:1）。
