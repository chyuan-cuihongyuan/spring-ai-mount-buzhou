# impl 1492 — QuantileAggregationBias 分位数聚合偏差审计（R92 = effort #1891 / spec 1891 / T2983-T2984）

**What**：`QuantileAggregationBias`（core/metrics 静态纯函数）——
naiveAverage（分位平均错误口径可示众）+ biasRatio（符号化偏差，
负=低报危险侧）+ isMateriallyBiased（|比|≥容差判定）；空表/负值/
actual=0/容差越界 fail-fast。

**Why**：Prometheus/M3 惯例分位不可加——分片 p99 平均与全体 p99
可差 −40%，SLA 报告 naive 口径的违约判定全错；偏差量入账后口径
有救。落轮 grep 复核卡方占坑换静脉。

**Verify**：`QuantileAggregationBiasTest` 4 用例全绿（经典 −0.4/
高报正侧/无偏与容差边界/畸形四型 fail-fast）。

**Status**：done（2026-09-23）
