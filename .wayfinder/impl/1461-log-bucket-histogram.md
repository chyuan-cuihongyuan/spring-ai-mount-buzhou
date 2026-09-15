# impl 1461 — LogBucketHistogram 对数分桶直方图（R61 = effort #1860 / spec 1860 / T2921-T2922）

**What**：`LogBucketHistogram`（core/metrics 静态纯函数）——bucketIndex
对数桶号 + buckets TreeMap 账 + quantile 桶序累计几何中点 →
ApproxQuantile（误差界 γ−1 随值返回）；正值域与畸形五型 fail-fast。

**Why**：HdrHistogram/DDSketch 思想——桶内相对差有界 → 分位数估计误差
有界声明；O(n) 一遍分桶免全排序、对数桶高段不失真（对照线性桶把
100ms 与 10s 同桶）。

**Verify**：`LogBucketHistogramTest` 4 用例全绿。

**Status**：done（2026-09-16）
