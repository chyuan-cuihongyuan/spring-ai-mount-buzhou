# impl 1339 — ExemptionTtlHistogram 豁免 TTL 直方（R40 = effort #1739 / spec 1739 / T2679-T2680）

**What**：五桶+permanent 独立计数（TTL<=0 永久）
**Why**：cert-manager 生命周期普查——权限漂移温床显形
**Verify**：ExemptionTtlHistogramTest 2 断言 全绿。 **Status**：done（2026-09-15）
