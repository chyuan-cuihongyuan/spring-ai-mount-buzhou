# impl 1343 — SpillHandleAgeHistogram 句柄驻留年龄直方（R44 = effort #1743 / spec 1743 / T2687-T2688）

**What**：四桶+eldest 哨戒+负值忽略
**Why**：Redis IDLETIME——onload 跟不上 offload 显形
**Verify**：SpillHandleAgeHistogramTest 2 断言 全绿。 **Status**：done（2026-09-15）
