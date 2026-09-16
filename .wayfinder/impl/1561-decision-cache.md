# impl 1561 — 判定决策缓存（spec 2010 / T3121–T3122 / R11）

纵切片：`DecisionCache`（buzhou-guard decision 主，新子包）+
`DecisionCacheTest`（八用例）。TTL 短路、惰性清除分计、LRU 驱逐、
invalidate、四计数命中率。

- 测试：`mvn -pl buzhou-guard test -Dtest=DecisionCacheTest` 8/8 绿。
