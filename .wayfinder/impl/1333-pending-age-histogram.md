# impl 1333 — PendingAgeHistogram 待决事件年龄直方（R34 = effort #1733 / spec 1733 / T2667-T2668）

**What**：桶式 1s/10s/1m/5m 五桶+oldest 哨戒
**Why**：Kafka lag exporter——管道实时 vs 积压显形
**Verify**：PendingAgeHistogramTest 3 断言 全绿。 **Status**：done（2026-09-15）
