# impl 1338 — PiiScanLatency 扫描耗时分位读面（R39 = effort #1738 / spec 1738 / T2677-T2678）

**What**：record 负值忽略+median/p95/max 分位+哨兵
**Why**：Envoy per-filter 计时——安全不能比漏洞更慢
**Verify**：PiiScanLatencyTest 3 断言 全绿。 **Status**：done（2026-09-15）
